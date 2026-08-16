package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import java.time.Instant;
import java.util.List;

public record
BriefingDetailResponse(
        Long briefingId,
        Long customerId,
        String customerName,
        String summaryText,
        String scriptText,
        String ttsAudioUrl,
        Integer ttsDuration,
        BriefingStatus status,
        Instant createdAt,
        ResolvedRequestResponse resolvedRequest,
        List<RecommendedProductResponse> recommendedProducts
) {

    public static BriefingDetailResponse of(
            Briefing briefing,
            ResolvedRequestResponse resolvedRequest,
            List<RecommendedProductResponse> recommendedProducts
    ) {
        return new BriefingDetailResponse(
                briefing.getId(),
                briefing.getCustomer().getId(),
                briefing.getCustomer().getCustomerName(),
                briefing.getSummaryText(),
                briefing.getScriptText(),
                briefing.getTtsAudioUrl(),
                briefing.getTtsDuration(),
                briefing.getStatus(),
                briefing.getCreatedAt(),
                resolvedRequest,
                recommendedProducts
        );
    }
}

