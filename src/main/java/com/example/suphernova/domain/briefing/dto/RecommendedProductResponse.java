package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.product.entity.Product;

public record RecommendedProductResponse(
        Long productId,
        String productName,
        Long price,
        String imageUrl,
        int matchScore
) {

    public static RecommendedProductResponse of(Product product, int matchScore) {
        return new RecommendedProductResponse(
                product.getId(),
                product.getProductName(),
                product.getPrice(),
                product.getImageUrl(),
                matchScore
        );
    }
}
