package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.client.AiBriefingGenerationClient;
import com.example.suphernova.domain.briefing.client.AiBriefingGenerationResult;
import com.example.suphernova.domain.briefing.client.ExternalCallbackStatus;
import com.example.suphernova.domain.briefing.client.SttClient;
import com.example.suphernova.domain.briefing.client.TtsClient;
import com.example.suphernova.domain.briefing.dto.BriefingAudioIngestResponse;
import com.example.suphernova.domain.briefing.dto.SttCallbackRequest;
import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.repository.BriefingRepository;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import com.example.suphernova.global.storage.AudioStorageService;
import com.example.suphernova.global.storage.StoredAudio;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 음성 녹음 업로드 → STT 전사 → 정보 추출/스크립트 생성 → TTS 합성으로 이어지는
 * 비동기 브리핑 생성 파이프라인. 단계별 설계는 docs/STT-TTS-INTEGRATION.md 참고.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingIngestionService {

    private final BriefingRepository briefingRepository;
    private final CustomerRepository customerRepository;
    private final KeywordRepository keywordRepository;
    private final AudioStorageService audioStorageService;
    private final SttClient sttClient;
    private final TtsClient ttsClient;
    private final AiBriefingGenerationClient aiBriefingGenerationClient;

    @Transactional
    public BriefingAudioIngestResponse ingestAudio(Long customerId, MultipartFile audio) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        String inputAudioUrl = storeAudio("input", audio);
        Briefing briefing = briefingRepository.save(Briefing.pending(customer, inputAudioUrl));

        try {
            String jobId = sttClient.requestTranscription(inputAudioUrl, briefing.getId());
            briefing.markProcessing(jobId);
        } catch (RestClientException e) {
            log.warn("STT 전사 요청 실패. briefingId={}", briefing.getId(), e);
            briefing.markFailed();
        }

        return BriefingAudioIngestResponse.from(briefing);
    }

    @Transactional
    public void handleSttCallback(SttCallbackRequest request) {
        Briefing briefing = briefingRepository.findById(request.briefingId())
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        if (request.status() == ExternalCallbackStatus.FAILED) {
            briefing.markFailed();
            return;
        }

        AiBriefingGenerationResult result = aiBriefingGenerationClient.generate(request.text());
        if (result.scriptText() == null || result.scriptText().isBlank()) {
            briefing.markNoHistory();
            return;
        }

        briefing.applyGeneratedContent(result.summaryText(), result.scriptText());
        saveKeywords(briefing.getCustomer(), result.keywords());

        try {
            ttsClient.requestSynthesis(result.scriptText(), briefing.getId());
        } catch (RestClientException e) {
            log.warn("TTS 합성 요청 실패. briefingId={}", briefing.getId(), e);
            briefing.markFailed();
        }
    }

    @Transactional
    public void handleTtsCallback(Long briefingId, ExternalCallbackStatus status, Integer durationSeconds, MultipartFile audio) {
        Briefing briefing = briefingRepository.findById(briefingId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        if (status == ExternalCallbackStatus.FAILED || audio == null || audio.isEmpty()) {
            briefing.markFailed();
            return;
        }

        String ttsAudioUrl = storeAudio("tts", audio, briefingId + fileExtension(audio));
        briefing.applyTtsResult(ttsAudioUrl, durationSeconds);
    }

    private void saveKeywords(Customer customer, List<String> keywords) {
        keywords.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> Keyword.builder().customer(customer).keywordName(name).build())
                .forEach(keywordRepository::save);
    }

    private String storeAudio(String directory, MultipartFile audio) {
        return storeAudio(directory, audio, UUID.randomUUID() + fileExtension(audio));
    }

    private String storeAudio(String directory, MultipartFile audio, String fileName) {
        try {
            StoredAudio stored = audioStorageService.store(directory, fileName, audio.getInputStream(), audio.getContentType());
            return stored.url();
        } catch (IOException e) {
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String fileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
