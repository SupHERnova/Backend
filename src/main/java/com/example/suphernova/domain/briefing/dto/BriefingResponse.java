package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import java.time.LocalDate;
import java.util.Arrays;

public record BriefingResponse(
        Long briefingId,
        Long customerId,
        String summaryText,
        String scriptText,
        String firstSentence,
        String ttsAudioUrl,
        Integer ttsDuration,
        BriefingStatus status,
        LocalDate createdAt
) {

    public static BriefingResponse from(Briefing briefing) {
        return new BriefingResponse(
                briefing.getId(),
                briefing.getCustomer().getId(),
                briefing.getSummaryText(),
                briefing.getScriptText(),
                extractFirstSentence(briefing.getScriptText()),
                briefing.getTtsAudioUrl(),
                briefing.getTtsDuration(),
                briefing.getStatus(),
                briefing.getCreatedAt()
        );
    }

    private static String extractFirstSentence(String scriptText) {
        if (scriptText == null || scriptText.isBlank()) {
            return null;
        }

        String normalized = scriptText.strip().replace("\n", " ");
        String[] sentences = normalized.split("(?<=[.!?])\\s*");

        return Arrays.stream(sentences)
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .filter(s -> !s.contains("안녕하세요"))
                .findFirst()
                .orElseGet(() -> sentences.length > 0 ? sentences[0].strip() : null);
    }
}
