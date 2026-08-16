package com.example.suphernova.domain.briefing.controller;

import com.example.suphernova.domain.briefing.dto.BriefingAudioIngestResponse;
import com.example.suphernova.domain.briefing.service.BriefingIngestionService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Briefing", description = "브리핑 API")
@RestController
@RequiredArgsConstructor
public class BriefingAudioController {

    private final BriefingIngestionService briefingIngestionService;

    @Operation(summary = "브리핑 음성 업로드", description = "고객 대화 녹음 파일을 업로드해 STT→AI 브리핑 생성→TTS 파이프라인을 비동기로 시작합니다.")
    @PostMapping(value = "/api/customers/{customerId}/briefings/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BriefingAudioIngestResponse>> ingestAudio(
            @PathVariable Long customerId,
            @RequestParam("audio") MultipartFile audio
    ) {
        BriefingAudioIngestResponse response = briefingIngestionService.ingestAudio(customerId, audio);
        return ResponseEntity.status(GeneralSuccessCode.ACCEPTED.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.ACCEPTED, response));
    }
}
