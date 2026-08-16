package com.example.suphernova.domain.record.dto;

import com.example.suphernova.domain.record.entity.Records;

import java.time.LocalDateTime;

public record RecordResponseDto(
        Long recordId,
        Long customerId,
        String rawNote,
        String aiSummary,
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