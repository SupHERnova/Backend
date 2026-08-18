package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.product.entity.Product;

public record ResolvedRequestResponse(
        Long productId,
        String productName,
        Integer restockQuantity,
        String restockMessage
) {

    public static ResolvedRequestResponse from(Product product) {
        return new ResolvedRequestResponse(
                product.getId(),
                product.getProductName(),
                product.getStockQuantity(),
                "오늘 %d개 재입고".formatted(product.getStockQuantity())
        );
    }
}
