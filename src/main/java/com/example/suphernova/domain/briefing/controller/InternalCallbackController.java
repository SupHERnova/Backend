package com.example.suphernova.domain.briefing.controller;

import com.example.suphernova.domain.briefing.client.ExternalCallbackStatus;
import com.example.suphernova.domain.briefing.dto.SttCallbackRequest;
import com.example.suphernova.domain.briefing.service.BriefingIngestionService;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import com.example.suphernova.global.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * STT/TTS 서비스가 비동기 작업 완료를 통지하는 서버-투-서버 전용 엔드포인트.
 * SA 앱 등 외부 클라이언트가 직접 호출해서는 안 되며, 공유 시크릿 헤더로 인증한다.
 */
@Tag(name = "Internal", description = "내부 콜백 API")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCallbackController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final BriefingIngestionService briefingIngestionService;
    private final AppProperties appProperties;

    @Operation(summary = "STT 전사 완료 콜백", description = "STT 서비스가 전사 결과를 통지합니다.")
    @PostMapping("/stt-callback")
    public ResponseEntity<Void> sttCallback(
            @RequestHeader(INTERNAL_TOKEN_HEADER) String token,
            @Valid @RequestBody SttCallbackRequest request
    ) {
        validateToken(token);
        briefingIngestionService.handleSttCallback(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "TTS 합성 완료 콜백", description = "TTS 서비스가 합성된 오디오 파일을 통지합니다.")
    @PostMapping(value = "/tts-callback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> ttsCallback(
            @RequestHeader(INTERNAL_TOKEN_HEADER) String token,
            @RequestParam Long briefingId,
            @RequestParam ExternalCallbackStatus status,
            @RequestParam(required = false) Integer durationSeconds,
            @RequestParam(required = false) MultipartFile audio
    ) {
        validateToken(token);
        briefingIngestionService.handleTtsCallback(briefingId, status, durationSeconds, audio);
        return ResponseEntity.noContent().build();
    }

    private void validateToken(String token) {
        if (!appProperties.internal().token().equals(token)) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }
    }
}
