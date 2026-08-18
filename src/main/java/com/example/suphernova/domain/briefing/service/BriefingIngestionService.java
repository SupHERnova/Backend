package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.client.AiBriefingGenerationClient;
import com.example.suphernova.domain.briefing.client.AiBriefingGenerationResult;
import com.example.suphernova.domain.briefing.client.TtsClient;
import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.repository.BriefingRepository;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import com.example.suphernova.global.storage.AudioDurationCalculator;
import com.example.suphernova.global.storage.AudioStorageService;
import com.example.suphernova.global.storage.StoredAudio;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SA 앱이 전사한 텍스트를 받아 AI 브리핑 생성(요약/스크립트/키워드) 후 TTS 음성까지
 * 한 번의 요청에서 동기적으로 완결하는 파이프라인. STT는 프론트에서 처리하므로 다루지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingIngestionService {

    private final BriefingRepository briefingRepository;
    private final CustomerRepository customerRepository;
    private final KeywordRepository keywordRepository;
    private final AudioStorageService audioStorageService;
    private final AudioDurationCalculator audioDurationCalculator;
    private final TtsClient ttsClient;
    private final AiBriefingGenerationClient aiBriefingGenerationClient;

    @Transactional
    public BriefingResponse ingestText(Long customerId, String transcribedText) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        Briefing briefing = Briefing.create(customer);

        AiBriefingGenerationResult result = aiBriefingGenerationClient.generate(transcribedText);
        if (result.scriptText() == null || result.scriptText().isBlank()) {
            briefing.markNoHistory();
            return BriefingResponse.from(briefingRepository.save(briefing));
        }

        briefing.applyGeneratedContent(result.summaryText(), result.scriptText());
        saveKeywords(customer, result.keywords());

        try {
            byte[] audio = ttsClient.synthesize(result.scriptText());
            String ttsAudioUrl = storeAudio(audio);
            Integer ttsDuration = audioDurationCalculator.calculateSeconds(audio);
            briefing.applyTtsResult(ttsAudioUrl, ttsDuration);
        } catch (Exception e) {
            log.warn("TTS 합성 실패. customerId={}", customerId, e);
            briefing.markFailed();
        }

        return BriefingResponse.from(briefingRepository.save(briefing));
    }

    private void saveKeywords(Customer customer, List<String> keywords) {
        keywords.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> Keyword.builder().customer(customer).keywordName(name).build())
                .forEach(keywordRepository::save);
    }

    private String storeAudio(byte[] audio) {
        String fileName = UUID.randomUUID() + ".mp3";
        try {
            StoredAudio stored = audioStorageService.store("tts", fileName, new ByteArrayInputStream(audio), "audio/mpeg");
            return stored.url();
        } catch (IOException e) {
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
