package com.example.suphernova.domain.record.service;

import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.recommendation.dto.OpenAiDto;
import com.example.suphernova.domain.record.dto.RecordRequestDto;
import com.example.suphernova.domain.record.dto.RecordResponseDto;
import com.example.suphernova.domain.record.entity.Records;
import com.example.suphernova.domain.record.repository.RecordRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private final CustomerRepository customerRepository;
    private final RecordRepository recordRepository;
    private final RestTemplate restTemplate;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    public RecordResponseDto createAndSaveRecord(Long customerId, RecordRequestDto requestDto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        String aiSummary = summarizeNoteWithAi(requestDto.rawNote());

        return saveRecordTransaction(customer, requestDto.rawNote(), aiSummary);
    }

    @Transactional
    public RecordResponseDto saveRecordTransaction(Customer customer, String rawNote, String aiSummary) {
        Records record = Records.builder()
                .customer(customer)
                .rawNote(rawNote)
                .aiSummary(aiSummary)
                .build();

        Records savedRecord = recordRepository.save(record);
        return RecordResponseDto.from(savedRecord);
    }

    private String summarizeNoteWithAi(String rawNote) {
        if (openAiApiKey == null || openAiApiKey.isBlank() || "mock-key".equals(openAiApiKey)) {
            log.warn("OpenAI API Key가 설정되지 않았거나 올바르지 않아 기본 rawNote를 반환합니다.");
            return rawNote;
        }

        try {
            String anonymizedNote = maskPersonalInfo(rawNote);

            String systemPrompt = "당신은 고급 명품 매장의 CRM 전담 AI 비서입니다. 판매원이 입력한 거친 수기 메모를 바탕으로 '관심 상품 및 착용 소감', '구매 주저 이유/요청 사항' 등을 명확하고 정돈된 핵심 문장(2~3문장)으로 간결하게 작성하세요.";
            String userPrompt = String.format("수기 메모: %s", anonymizedNote);

            OpenAiDto.Request requestBody = new OpenAiDto.Request(
                    model,
                    List.of(
                            new OpenAiDto.Message("system", systemPrompt),
                            new OpenAiDto.Message("user", userPrompt)
                    ),
                    0.5
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            HttpEntity<OpenAiDto.Request> entity = new HttpEntity<>(requestBody, headers);
            OpenAiDto.Response response = restTemplate.postForObject(openAiApiUrl, entity, OpenAiDto.Response.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content().trim();
            }
            return rawNote;
        } catch (Exception e) {
            log.error("AI 요약 처리 중 오류 발생 (Fallback rawNote 적용): ", e);
            return rawNote;
        }
    }

    /**
     * 외부 AI API 전송 전 개인식별정보(PII) 비식별화
     */
    private String maskPersonalInfo(String text) {
        if (text == null) return "";
        // 1. 전화번호 마스킹 (예: 010-1234-5678 -> 010-****-****)
        String masked = text.replaceAll("(01[016789])[-.\\s]?(\\d{3,4})[-.\\s]?(\\d{4})", "$1-****-****");
        // 2. 이메일 로컬 파트 마스킹 (예: test@example.com -> t***@example.com)
        // 이메일 토큰 단어 경계(\\b) 내에서 첫 글자만 유지하고 로컬 파트 잔여 영역만 마스킹
        masked = masked.replaceAll("(?i)\\b([a-z0-9._%+-])([a-z0-9._%+-]+)(@[a-z0-9.-]+\\.[a-z]{2,})\\b", "$1***$3");
        return masked;
    }
}