package com.example.suphernova.domain.recommendation.dto;

import java.util.List;

public record RecommendationResponse(
        RestockedProductDto restockedProduct,           // 1. 재입고 / 고일치 상품 (Top 1)
        List<MatchedProductDto> matchedProducts,          // 2. 취향 매칭 추천 상품 (Top 3)
        SimilarCustomerStatsDto similarCustomerStats,     // 3. 비슷한 취향의 고객 데이터
        String recommendationComment                       // 4. AI 추천 첫 멘트
) {
    public record RestockedProductDto(
            Long productId,
            String productName,
            String size,
            Integer restockedCount,
            String matchReason
    ) {}

    public record MatchedProductDto(
            Long productId,
            String productName,
            Long price,
            String imageUrl,
            Integer matchRate
    ) {}

    public record SimilarCustomerStatsDto(
            String topCategoryName,
            List<CategoryPurchaseRatioDto> ratios
    ) {}

    public record CategoryPurchaseRatioDto(
            String categoryName,
            Integer ratio
    ) {}
}