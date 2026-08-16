package com.example.suphernova.domain.record.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordRequestDto(
        @NotBlank(message = "수기 메모 내용은 필수 입력 항목입니다.")
        String rawNote
) {}