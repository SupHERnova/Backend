package com.example.suphernova.domain.briefing.controller;

import com.example.suphernova.domain.briefing.dto.BriefingCreateRequest;
import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.briefing.service.BriefingService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Briefing", description = "브리핑 API")
@RestController
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;

    @Operation(summary = "브리핑 등록", description = "고객에 대한 브리핑 결과를 저장합니다.")
    @PostMapping("/api/briefings")
    public ResponseEntity<ApiResponse<BriefingResponse>> createBriefing(
            @Valid @RequestBody BriefingCreateRequest request
    ) {
        BriefingResponse response = briefingService.createBriefing(request);
        return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.CREATED, response));
    }

    @Operation(summary = "고객별 브리핑 목록 조회", description = "고객의 브리핑 목록을 최신순으로 조회합니다.")
    @GetMapping("/api/customers/{customerId}/briefings")
    public ResponseEntity<ApiResponse<List<BriefingResponse>>> getBriefingsByCustomer(
            @PathVariable Long customerId
    ) {
        List<BriefingResponse> response = briefingService.getBriefingsByCustomer(customerId);
        return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.OK, response));
    }

    @Operation(summary = "브리핑 단건 조회", description = "브리핑 하나의 정보를 조회합니다.")
    @GetMapping("/api/briefings/{briefingId}")
    public ResponseEntity<ApiResponse<BriefingResponse>> getBriefing(
            @PathVariable Long briefingId
    ) {
        BriefingResponse response = briefingService.getBriefing(briefingId);
        return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.OK, response));
    }
}
