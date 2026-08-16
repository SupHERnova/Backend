package com.example.suphernova.domain.briefing.client;

import java.util.List;

public record AiBriefingGenerationResult(
        String summaryText,
        String scriptText,
        List<String> keywords
) {
}
