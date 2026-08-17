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
    private final RestTemplate restTemplate; // Config 빈 주입

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    // 외부 API 호출 구간이므로 전체 메서드에는 @Transactional을 붙이지 않음
    public RecordResponseDto createAndSaveRecord(Long customerId, RecordRequestDto requestDto) {
        // 1. 고객 존재 여부 단순 조회 (readOnly 트랜잭션 권장)
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        // 2. 외부 AI API 호출 (DB 트랜잭션 밖에서 실행)
        String aiSummary = summarizeNoteWithAi(requestDto.rawNote());

        // 3. DB 저장 수행 (별도 헬퍼 메서드로 트랜잭션 내에서 처리)
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
            log.warn("OpenAI API Key가 설정되지 않아 기본 rawNote를 반환합니다.");
            return rawNote;
        }

        try {
            String systemPrompt = "당신은 고급 명품 매장의 CRM 전담 AI 비서입니다. 판매원이 입력한 거친 수기 메모를 바탕으로 '관심 상품 및 착용 소감', '구매 주저 이유/요청 사항' 등을 명확하고 정돈된 핵심 문장(2~3문장)으로 간결하게 작성하세요.";
            // 개인정보 유출 방지: 고객 실명 대신 비식별 표현 사용
            String userPrompt = String.format("수기 메모: %s", rawNote);

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
}