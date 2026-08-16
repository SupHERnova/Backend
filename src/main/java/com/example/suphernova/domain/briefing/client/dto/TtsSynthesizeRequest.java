package com.example.suphernova.domain.briefing.client.dto;

public record TtsSynthesizeRequest(
        String text,
        String callbackUrl,
        Long referenceId
) {
}
