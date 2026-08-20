package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.client.AiBriefingGenerationClient;
import com.example.suphernova.domain.briefing.client.AiBriefingGenerationResult;
import com.example.suphernova.domain.briefing.client.TtsClient;
import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.briefing.dto.PreferredKeywordsResponse;
import com.example.suphernova.domain.briefing.dto.RecentPurchaseResponse;
import com.example.suphernova.domain.briefing.dto.UnresolvedRequestResponse;
import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.repository.BriefingRepository;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.domain.order.entity.OrderItem;
import com.example.suphernova.domain.order.repository.OrderItemRepository;
import com.example.suphernova.domain.product.entity.ProductKeyword;
import com.example.suphernova.domain.product.entity.TagCategory;
import com.example.suphernova.domain.product.repository.ProductKeywordRepository;
import com.example.suphernova.domain.recommendation.service.RecommendationService;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import com.example.suphernova.global.storage.AudioDurationCalculator;
import com.example.suphernova.global.storage.AudioStorageService;
import com.example.suphernova.global.storage.StoredAudio;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고객의 현재 추천 상태(선호 키워드/최근구매/재입고 여부)를 바탕으로 AI 브리핑(요약/스크립트)과
 * TTS 음성을 생성해 저장한다. 별도 컨트롤러로 노출되지 않는 백엔드 내부 전용 컴포넌트로,
 * 추천 상태 반영이 끝난 뒤 {@link BriefingIngestionService}에서만 호출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingGenerationService {

    private final BriefingRepository briefingRepository;
    private final CustomerRepository customerRepository;
    private final KeywordRepository keywordRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductKeywordRepository productKeywordRepository;
    private final AudioStorageService audioStorageService;
    private final AudioDurationCalculator audioDurationCalculator;
    private final TtsClient ttsClient;
    private final AiBriefingGenerationClient aiBriefingGenerationClient;
    private final RecommendationService recommendationService;

    @Transactional
    public BriefingResponse generateFromRecommendation(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        String narrative = buildRecommendationNarrative(customer);

        Briefing briefing = Briefing.create(customer);

        AiBriefingGenerationResult result = aiBriefingGenerationClient.generate(narrative);
        if (result.scriptText() == null || result.scriptText().isBlank()) {
            briefing.markNoHistory();
            return BriefingResponse.from(briefingRepository.save(briefing));
        }

        briefing.applyGeneratedContent(result.summaryText(), result.scriptText());

        try {
            byte[] audio = ttsClient.synthesize(result.scriptText());
            String ttsAudioUrl = storeAudio(audio);
            Integer ttsDuration = audioDurationCalculator.calculateSeconds(audio);
            briefing.applyTtsResult(ttsAudioUrl, ttsDuration);
        } catch (Exception e) {
            log.warn("TTS 합성 실패. customerId={}", customerId, e);
            briefing.markFailed();
        }

        return BriefingResponse.from(briefingRepository.save(briefing));
    }

    /**
     * 현재 추천 상태(취향 키워드/최근구매/재입고 여부)를 AI 브리핑 생성용 원문으로 직렬화한다.
     */
    private String buildRecommendationNarrative(Customer customer) {
        Long customerId = customer.getId();

        PreferredKeywordsResponse preferredKeywords =
                PreferredKeywordsResponse.from(keywordRepository.findAllByCustomerId(customerId));
        List<RecentPurchaseResponse> recentPurchases = getRecentPurchases(customerId);
        UnresolvedRequestResponse unresolvedRequest =
                UnresolvedRequestResponse.from(recommendationService.getRestockedRequestProduct(customerId));

        StringBuilder sb = new StringBuilder();
        sb.append(customer.getCustomerName()).append(" 고객님 방문 전 브리핑입니다. ");

        if (preferredKeywords.brand() != null || preferredKeywords.color() != null
                || preferredKeywords.material() != null || preferredKeywords.mood() != null) {
            sb.append("선호 브랜드는 ").append(nullToNone(preferredKeywords.brand()))
                    .append(", 컬러는 ").append(nullToNone(preferredKeywords.color()))
                    .append(", 소재는 ").append(nullToNone(preferredKeywords.material()))
                    .append(", 무드는 ").append(nullToNone(preferredKeywords.mood())).append("입니다. ");
        }

        if (unresolvedRequest.resolved()) {
            sb.append(unresolvedRequest.productName()).append(" 상품이 ")
                    .append(unresolvedRequest.restockedCount()).append("개 재입고되었습니다. ");
        } else {
            sb.append("해결되지 않은 재입고 요청은 없습니다. ");
        }

        if (!recentPurchases.isEmpty()) {
            sb.append("최근 구매 상품은 ").append(recentPurchases.get(0).productName()).append("입니다.");
        }

        return sb.toString();
    }

    private String nullToNone(String value) {
        return value == null ? "없음" : value;
    }

    private List<RecentPurchaseResponse> getRecentPurchases(Long customerId) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrder_Customer_IdOrderByOrder_CreatedAtDesc(customerId);
        if (orderItems.isEmpty()) {
            return List.of();
        }

        List<Long> purchasedProductIds = orderItems.stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .toList();

        Map<Long, String> colorByProductId = productKeywordRepository.findAllByProductIdIn(purchasedProductIds).stream()
                .filter(pk -> pk.getTagCategory() == TagCategory.COLOR && pk.getTagName() != null)
                .collect(Collectors.groupingBy(
                        pk -> pk.getProduct().getId(),
                        Collectors.mapping(ProductKeyword::getTagName, Collectors.joining(" · "))
                ));

        return orderItems.stream()
                .map(item -> new RecentPurchaseResponse(
                        item.getOrder().getId(),
                        item.getOrder().getCreatedAt(),
                        item.getProduct().getProductName(),
                        colorByProductId.get(item.getProduct().getId()),
                        item.getProduct().getPrice()
                ))
                .toList();
    }

    private String storeAudio(byte[] audio) {
        String fileName = UUID.randomUUID() + ".mp3";
        try {
            StoredAudio stored = audioStorageService.store("tts", fileName, new ByteArrayInputStream(audio), "audio/mpeg");
            return stored.url();
        } catch (IOException e) {
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
