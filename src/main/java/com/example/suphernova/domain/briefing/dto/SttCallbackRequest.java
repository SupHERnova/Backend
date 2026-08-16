package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.briefing.client.ExternalCallbackStatus;
import jakarta.validation.constraints.NotNull;

public record SttCallbackRequest(

        String jobId,

        @NotNull(message = "브리핑 ID는 필수입니다.")
        Long briefingId,

        @NotNull(message = "상태는 필수입니다.")
        ExternalCallbackStatus status,

        String text
) {
}
