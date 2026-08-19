package com.example.suphernova.domain.briefing.dto;

import java.time.LocalDate;

public record RecentPurchaseResponse(
        Long orderId,
        LocalDate purchasedAt,
        String productName,
        String color,
        Long price
) {
}
