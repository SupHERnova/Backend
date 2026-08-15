package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import java.time.Instant;

public record BriefingResponse(
        Long briefingId,
        Long customerId,
        String summaryText,
        String scriptText,
        String ttsAudioUrl,
        Integer ttsDuration,
        BriefingStatus status,
        Instant createdAt
) {

    public static BriefingResponse from(Briefing briefing) {
        return new BriefingResponse(
                briefing.getId(),
                briefing.getCustomer().getId(),
                briefing.getSummaryText(),
                briefing.getScriptText(),
                briefing.getTtsAudioUrl(),
                briefing.getTtsDuration(),
                briefing.getStatus(),
                briefing.getCreatedAt()
        );
    }
}
