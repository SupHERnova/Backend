package com.example.suphernova.domain.record.controller;

import com.example.suphernova.domain.record.dto.RecordRequestDto;
import com.example.suphernova.domain.record.dto.RecordResponseDto;
import com.example.suphernova.domain.record.service.RecordService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Record", description = "고객 상담 기록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers/{customerId}/records")
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "상담 기록 저장 및 AI 요약", description = "수기 또는 STT로 입력된 메모를 AI로 요약하여 저장합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<RecordResponseDto>> createRecord(
            @PathVariable Long customerId,
            @RequestBody RecordRequestDto requestDto
    ) {
        RecordResponseDto response = recordService.createRecord(customerId, requestDto);
        return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.OK, response));
    }
}