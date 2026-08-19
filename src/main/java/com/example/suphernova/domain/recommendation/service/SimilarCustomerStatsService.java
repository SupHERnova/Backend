package com.example.suphernova.domain.recommendation.service;

import com.example.suphernova.domain.product.repository.ProductRepository;
import com.example.suphernova.domain.recommendation.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarCustomerStatsService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public RecommendationResponse.SimilarCustomerStatsDto getSimilarCustomerStats(Long customerId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return createEmptyStats("NO_KEYWORDS", "고객의 취향 키워드가 설정되어 있지 않습니다.");
        }

        try {
            Long similarCount = productRepository.countSimilarCustomers(customerId, keywords);
            if (similarCount == null || similarCount < 5) {
                return createEmptyStats("INSUFFICIENT_CUSTOMERS", "유사 고객의 데이터가 5건 이상 필요합니다.");
            }

            // CustomOrder 엔티티의 createdAt 타입(LocalDate)에 맞춰 조작
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            var stats = productRepository.findSimilarCustomerPurchaseStats(customerId, keywords, thirtyDaysAgo);

            if (stats == null || stats.isEmpty()) {
                return createEmptyStats("NO_PURCHASE_HISTORY", "최근 30일간 유사 고객의 구매 데이터가 존재하지 않습니다.");
            }

            long total = stats.stream().mapToLong(ProductRepository.ProductStatProjection::getPurchaseCount).sum();
            if (total == 0) {
                return createEmptyStats("NO_PURCHASE_HISTORY", "최근 30일간 유사 고객의 구매 데이터가 존재하지 않습니다.");
            }

            List<RecommendationResponse.ProductPurchaseRatioDto> ratios = stats.stream()
                    .map(s -> new RecommendationResponse.ProductPurchaseRatioDto(
                            s.getProductName(),
                            (int) Math.round(((double) s.getPurchaseCount() / total) * 100)
                    ))
                    .limit(2)
                    .toList();

            String topProductName = ratios.isEmpty() ? "인기 상품" : ratios.get(0).productName();
            return new RecommendationResponse.SimilarCustomerStatsDto(
                    true,
                    "SUCCESS",
                    "정상 조회되었습니다.",
                    topProductName,
                    ratios
            );

        } catch (Exception e) {
            log.error("유사 고객 통계 집계 중 예외 발생: ", e);
            return createEmptyStats("SYSTEM_ERROR", "유사 고객 통계를 불러오는 중 오류가 발생했습니다.");
        }
    }

    private RecommendationResponse.SimilarCustomerStatsDto createEmptyStats(String reasonCode, String message) {
        return new RecommendationResponse.SimilarCustomerStatsDto(
                false,
                reasonCode,
                message,
                null,
                List.of()
        );
    }
}