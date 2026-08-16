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
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Transactional
    public RecordResponseDto createAndSaveRecord(Long customerId, RecordRequestDto requestDto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        // 1. 수기 메모를 AI로 정돈/요약
        String aiSummary = summarizeNoteWithAi(customer.getCustomerName(), requestDto.rawNote());

        // 2. Records 엔티티 생성 및 DB 저장
        Records record = Records.builder()
                .customer(customer)
                .rawNote(requestDto.rawNote())
                .aiSummary(aiSummary)
                .build();

        Records savedRecord = recordRepository.save(record);

        // 3. DTO 변환 및 반환
        return RecordResponseDto.from(savedRecord);
    }

    private String summarizeNoteWithAi(String customerName, String rawNote) {
        try {
            String systemPrompt = "당신은 고급 명품 매장의 CRM 전담 AI 비서입니다. 판매원이 입력한 거친 수기 메모를 바탕으로 '관심 상품 및 착용 소감', '구매 주저 이유/요청 사항' 등을 명확하고 정돈된 핵심 문장(2~3문장)으로 간결하게 작성하세요.";
            String userPrompt = String.format("고객명: %s\n수기 메모: %s", customerName, rawNote);

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
            log.error("AI 요약 처리 중 오류 발생: ", e);
            return rawNote; // AI 실패 시 rawNote를 그대로 fallback 저장
        }
    }
}