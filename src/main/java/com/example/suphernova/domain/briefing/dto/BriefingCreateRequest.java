package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BriefingCreateRequest(

        @NotNull(message = "고객 ID는 필수입니다.")
        Long customerId,

        String summaryText,

        String scriptText,

        @Size(max = 500, message = "TTS 오디오 URL은 500자 이하여야 합니다.")
        String ttsAudioUrl,

        Integer ttsDuration,

        BriefingStatus status
) {
}
