package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import java.time.LocalDate;

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

        String firstLine = scriptText.strip().split("\n", 2)[0].strip();
        int endIndex = -1;
        for (char terminator : new char[]{'.', '!', '?'}) {
            int index = firstLine.indexOf(terminator);
            if (index >= 0 && (endIndex == -1 || index < endIndex)) {
                endIndex = index;
            }
        }

        return endIndex == -1 ? firstLine : firstLine.substring(0, endIndex + 1);
    }
}
