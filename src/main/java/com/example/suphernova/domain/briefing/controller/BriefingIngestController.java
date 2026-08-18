package com.example.suphernova.domain.briefing.controller;

import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.briefing.dto.BriefingTextIngestRequest;
import com.example.suphernova.domain.briefing.service.BriefingIngestionService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Briefing", description = "브리핑 API")
@RestController
@RequiredArgsConstructor
public class BriefingIngestController {

    private final BriefingIngestionService briefingIngestionService;

    @Operation(summary = "브리핑 생성(전사 텍스트 기반)",
            description = "프론트에서 전사한 텍스트를 받아 AI 브리핑 생성과 TTS 합성을 동기로 수행합니다.")
    @PostMapping("/api/customers/{customerId}/briefings/generate")
    public ResponseEntity<ApiResponse<BriefingResponse>> ingestText(
            @PathVariable Long customerId,
            @Valid @RequestBody BriefingTextIngestRequest request
    ) {
        BriefingResponse response = briefingIngestionService.ingestText(customerId, request.transcribedText());
        return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.CREATED, response));
    }
}
