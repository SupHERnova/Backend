package com.example.suphernova.domain.briefing.dto;

import jakarta.validation.constraints.NotBlank;

public record BriefingTextIngestRequest(

        @NotBlank(message = "전사된 텍스트는 필수입니다.")
        String transcribedText
) {
}
