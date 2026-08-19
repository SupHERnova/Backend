package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.recommendation.dto.RecommendationResponse;

public record UnresolvedRequestResponse(
        boolean resolved,
        Long productId,
        String productName,
        Integer restockedCount
) {

    public static UnresolvedRequestResponse from(RecommendationResponse.RestockedProductDto restockedProduct) {
        if (restockedProduct == null) {
            return new UnresolvedRequestResponse(false, null, null, null);
        }

        return new UnresolvedRequestResponse(
                true,
                restockedProduct.productId(),
                restockedProduct.productName(),
                restockedProduct.restockedCount()
        );
    }
}
