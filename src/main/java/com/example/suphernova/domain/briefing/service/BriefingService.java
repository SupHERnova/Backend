package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.dto.BriefingCreateRequest;
import com.example.suphernova.domain.briefing.dto.BriefingDetailResponse;
import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.briefing.dto.RecommendedProductResponse;
import com.example.suphernova.domain.briefing.dto.ResolvedRequestResponse;
import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import com.example.suphernova.domain.briefing.repository.BriefingRepository;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.domain.product.entity.Product;
import com.example.suphernova.domain.product.repository.ProductKeywordRepository;
import com.example.suphernova.domain.product.repository.ProductRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BriefingService {

    private static final int RECOMMENDED_PRODUCT_LIMIT = 5;

    private final BriefingRepository briefingRepository;
    private final CustomerRepository customerRepository;
    private final KeywordRepository keywordRepository;
    private final ProductRepository productRepository;
    private final ProductKeywordRepository productKeywordRepository;

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

    @Transactional(readOnly = true)
    public BriefingDetailResponse getBriefingDetail(Long briefingId) {
        Briefing briefing = briefingRepository.findById(briefingId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        Customer customer = briefing.getCustomer();
        Set<String> preferredKeywords = keywordRepository.findAllByCustomerId(customer.getId()).stream()
                .map(Keyword::getKeywordName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<Product> storeProducts = productRepository.findAllByStoreId(customer.getStore().getId());
        if (preferredKeywords.isEmpty() || storeProducts.isEmpty()) {
            return BriefingDetailResponse.of(briefing, null, List.of());
        }

        Map<Long, Set<String>> tagsByProductId = productKeywordRepository
                .findAllByProductIdIn(storeProducts.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.groupingBy(
                        pk -> pk.getProduct().getId(),
                        Collectors.mapping(
                                pk -> pk.getTagName() == null ? "" : pk.getTagName().toLowerCase(Locale.ROOT),
                                Collectors.toSet())
                ));

        List<ScoredProduct> scoredProducts = storeProducts.stream()
                .map(product -> new ScoredProduct(
                        product,
                        matchScore(preferredKeywords, tagsByProductId.getOrDefault(product.getId(), Set.of()))
                ))
                .filter(scored -> scored.matchScore() > 0)
                .sorted(Comparator.comparingInt(ScoredProduct::matchScore).reversed())
                .toList();

        ResolvedRequestResponse resolvedRequest = scoredProducts.stream()
                .map(ScoredProduct::product)
                .filter(this::isRestockedToday)
                .findFirst()
                .map(ResolvedRequestResponse::from)
                .orElse(null);

        List<RecommendedProductResponse> recommendedProducts = scoredProducts.stream()
                .filter(scored -> resolvedRequest == null || !scored.product().getId().equals(resolvedRequest.productId()))
                .limit(RECOMMENDED_PRODUCT_LIMIT)
                .map(scored -> RecommendedProductResponse.of(scored.product(), scored.matchScore()))
                .toList();

        return BriefingDetailResponse.of(briefing, resolvedRequest, recommendedProducts);
    }

    private boolean isRestockedToday(Product product) {
        if (product.getStockQuantity() == null || product.getStockQuantity() <= 0 || product.getRestockedAt() == null) {
            return false;
        }
        LocalDate restockedDate = product.getRestockedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        return restockedDate.equals(LocalDate.now(ZoneId.systemDefault()));
    }

    private int matchScore(Set<String> preferredKeywords, Set<String> productTags) {
        long overlap = productTags.stream().filter(preferredKeywords::contains).count();
        return Math.round(100f * overlap / preferredKeywords.size());
    }

    private record ScoredProduct(Product product, int matchScore) {
    }
}
