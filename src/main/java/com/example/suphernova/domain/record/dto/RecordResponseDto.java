package com.example.suphernova.domain.record.dto;

import com.example.suphernova.domain.record.entity.Records;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record RecordResponseDto(
        Long recordId,
        Long customerId,
        String rawNote,
        String aiSummary,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static RecordResponseDto from(Records record) {
        return new RecordResponseDto(
                record.getId(),
                record.getCustomer().getId(),
                record.getRawNote(),
                record.getAiSummary(),
                record.getCreatedAt()
        );
    }
}