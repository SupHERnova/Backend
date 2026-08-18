package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.dto.BriefingContextResponse;
import com.example.suphernova.domain.briefing.dto.BriefingCreateRequest;
import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.briefing.dto.PreferredKeywordsResponse;
import com.example.suphernova.domain.briefing.dto.RecentPurchaseResponse;
import com.example.suphernova.domain.briefing.dto.UnresolvedRequestResponse;
import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import com.example.suphernova.domain.briefing.repository.BriefingRepository;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.domain.order.entity.OrderItem;
import com.example.suphernova.domain.order.repository.OrderItemRepository;
import com.example.suphernova.domain.product.entity.ProductKeyword;
import com.example.suphernova.domain.product.entity.TagCategory;
import com.example.suphernova.domain.product.repository.ProductKeywordRepository;
import com.example.suphernova.domain.recommendation.dto.RecommendationResponse;
import com.example.suphernova.domain.recommendation.service.RecommendationService;
import com.example.suphernova.domain.recommendation.service.SimilarCustomerStatsService;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BriefingService {

    private final BriefingRepository briefingRepository;
    private final CustomerRepository customerRepository;
    private final KeywordRepository keywordRepository;
    private final ProductKeywordRepository productKeywordRepository;
    private final OrderItemRepository orderItemRepository;
    private final SimilarCustomerStatsService similarCustomerStatsService;
    private final RecommendationService recommendationService;
    private final BriefingIngestionService briefingIngestionService;

    @Transactional
    public BriefingResponse createBriefing(BriefingCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        Briefing briefing = Briefing.builder()
                .customer(customer)
                .summaryText(request.summaryText())
                .scriptText(request.scriptText())
                .ttsAudioUrl(request.ttsAudioUrl())
                .ttsDuration(request.ttsDuration())
                .status(request.status() != null ? request.status() : BriefingStatus.SUCCESS)
                .build();

        return BriefingResponse.from(briefingRepository.save(briefing));
    }

    @Transactional(readOnly = true)
    public List<BriefingResponse> getBriefingsByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ProjectException(GeneralErrorCode.NOT_FOUND);
        }

        return briefingRepository.findAllByCustomerIdOrderByCreatedAtDescIdDesc(customerId).stream()
                .map(BriefingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BriefingResponse getBriefing(Long briefingId) {
        return briefingRepository.findById(briefingId)
                .map(BriefingResponse::from)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
    }

    @Transactional
    public BriefingContextResponse getBriefingContext(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        List<Keyword> customerKeywordEntities = keywordRepository.findAllByCustomerId(customerId);
        PreferredKeywordsResponse preferredKeywords = PreferredKeywordsResponse.from(customerKeywordEntities);
        List<RecentPurchaseResponse> recentPurchases = getRecentPurchases(customerId);

        List<String> rawKeywordNames = customerKeywordEntities.stream()
                .map(Keyword::getKeywordName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
        RecommendationResponse.SimilarCustomerStatsDto similarCustomerStats =
                similarCustomerStatsService.getSimilarCustomerStats(customerId, rawKeywordNames);
        UnresolvedRequestResponse unresolvedRequest =
                UnresolvedRequestResponse.from(recommendationService.getRestockedRequestProduct(customerId));

        String narrative = buildContextNarrative(customer, preferredKeywords, recentPurchases, unresolvedRequest);
        BriefingResponse briefing = briefingIngestionService.ingestText(customerId, narrative);

        return new BriefingContextResponse(
                briefing.briefingId(),
                customer.getId(),
                customer.getCustomerName(),
                preferredKeywords,
                recentPurchases,
                similarCustomerStats,
                unresolvedRequest
        );
    }

    /**
     * 화면에 보여줄 컨텍스트 데이터를 AI 브리핑 생성용 원문으로 직렬화한다.
     * SA의 실시간 상담 전사문이 없는 시점(방문 전 브리핑)에도 TTS 브리핑을 만들 수 있도록,
     * 컨텍스트 자체를 {@link BriefingIngestionService}의 입력으로 재사용한다.
     */
    private String buildContextNarrative(
            Customer customer,
            PreferredKeywordsResponse preferredKeywords,
            List<RecentPurchaseResponse> recentPurchases,
            UnresolvedRequestResponse unresolvedRequest
    ) {
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

}
