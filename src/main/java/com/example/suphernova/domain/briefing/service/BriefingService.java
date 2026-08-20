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

        Long latestBriefingId = briefingRepository.findAllByCustomerIdOrderByCreatedAtDescIdDesc(customerId).stream()
                .findFirst()
                .map(Briefing::getId)
                .orElse(null);

        return new BriefingContextResponse(
                latestBriefingId,
                customer.getId(),
                customer.getCustomerName(),
                preferredKeywords,
                recentPurchases,
                similarCustomerStats,
                unresolvedRequest
        );
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
