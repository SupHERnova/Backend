// RecordController.java
package com.example.suphernova.domain.record.controller;

import com.example.suphernova.domain.record.dto.RecordRequestDto;
import com.example.suphernova.domain.record.dto.RecordResponseDto;
import com.example.suphernova.domain.record.service.RecordService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Record", description = "고객 상담 기록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "고객 상담 기록 생성", description = "수기 메모를 AI로 정리하여 상담 기록을 저장합니다.")
    @PostMapping("/{customerId}/records")
    public ApiResponse<RecordResponseDto> createRecord(
            @PathVariable Long customerId,
            @Valid @RequestBody RecordRequestDto requestDto) {

        RecordResponseDto response = recordService.createAndSaveRecord(customerId, requestDto);
        return ApiResponse.of(GeneralSuccessCode.OK, response);
    }
}