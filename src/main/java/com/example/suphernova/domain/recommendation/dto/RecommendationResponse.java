package com.example.suphernova.domain.recommendation.dto;

import java.util.List;

public record RecommendationResponse(
        RestockedProductDto restockedProduct,           // 1. 재입고 / 고일치 상품 (Top 1)
        List<MatchedProductDto> matchedProducts,          // 2. 취향 매칭 추천 상품 (Top 3)
        SimilarCustomerStatsDto similarCustomerStats,     // 3. 비슷한 취향의 고객 데이터 (사유 코드 포함)
        String recommendationComment                       // 4. AI 추천 첫 멘트
) {
    public record RestockedProductDto(
            Long productId,
            String productName,
            String size,
            Integer restockedCount,
            String slotReason
    ) {}

    public record MatchedProductDto(
            Long productId,
            String productName,
            Long price,
            String imageUrl,
            Integer matchRate
    ) {}

    public record SimilarCustomerStatsDto(
            boolean isAvailable,          // 데이터 제공 가능 여부 (true/false)
            String reasonCode,           // 프론트 분기용 코드 (e.g. "INSUFFICIENT_CUSTOMERS", "NO_PURCHASE_HISTORY")
            String reasonMessage,        // 프론트 노출용 안내 메시지
            String topProductName,
            List<ProductPurchaseRatioDto> ratios
    ) {}

    public record ProductPurchaseRatioDto(
            String productName,
            Integer ratio
    ) {}
}