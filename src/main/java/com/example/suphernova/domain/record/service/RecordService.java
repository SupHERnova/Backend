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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;
    private final CustomerRepository customerRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Transactional
    public RecordResponseDto createRecord(Long customerId, RecordRequestDto requestDto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        // 1. LLM을 통한 텍스트 요약 및 정리 (실패 시 원본 저장 Fallback)
        String aiSummary = summarizeNote(requestDto.rawNote());

        // 2. DB에 기록 저장
        Records record = Records.builder()
                .customer(customer)
                .rawNote(requestDto.rawNote())
                .aiSummary(aiSummary)
                .build();

        recordRepository.save(record);

        return RecordResponseDto.from(record);
    }

    private String summarizeNote(String rawNote) {
        // API 키 미설정 시 원본 메모 그대로 반환
        if (openAiApiKey == null || openAiApiKey.isBlank() || "mock-key".equals(openAiApiKey)) {
            return rawNote;
        }

        try {
            String systemPrompt = """
                당신은 명품 매장 직원(SA)의 고객 상담 메모를 깔끔하게 요약해주는 AI 보조입니다.
                전달받은 메모를 바탕으로 아래 형식에 맞춰 핵심만 가독성 좋게 정리해 주세요:
                - 관심 상품 및 반응
                - 망설인 이유 또는 다음 연락/방문 시점
                - 요청 사항 (사이즈, 컬러 등)
                (해당하는 내용이 메모에 없으면 해당 항목은 생략하고 불릿 포인트 형태로 간결히 정리하세요.)
                """;

            OpenAiDto.Request requestBody = new OpenAiDto.Request(
                    model,
                    List.of(
                            new OpenAiDto.Message("system", systemPrompt),
                            new OpenAiDto.Message("user", rawNote)
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
            return rawNote; // 요약 실패 시 원본 전달
        } catch (Exception e) {
            return rawNote; // API 오류 시 원본 전달하여 저장 성공 보장
        }
    }
}