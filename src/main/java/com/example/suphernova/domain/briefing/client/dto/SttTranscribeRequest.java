package com.example.suphernova.domain.briefing.client.dto;

public record SttTranscribeRequest(
        String audioUrl,
        String callbackUrl,
        Long referenceId
) {
}
