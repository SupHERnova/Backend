package com.example.suphernova.domain.briefing.client;

import com.example.suphernova.domain.briefing.client.RecommendationExtractionResult.ExtractedKeyword;
import com.example.suphernova.domain.product.entity.TagCategory;
import com.example.suphernova.domain.recommendation.dto.OpenAiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 전사 텍스트를 OpenAI Chat Completions에 전달해 선호 키워드(브랜드/컬러/소재/무드)와
 * 재입고 요청 여부만 구조화된 JSON으로 추출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiRecommendationExtractionClient implements AiRecommendationExtractionClient {

    private static final int KEYWORD_NAME_MAX_LENGTH = 10;
    private static final int PRODUCT_NAME_MAX_LENGTH = 100;

    private static final String SYSTEM_PROMPT = """
            당신은 명품 매장의 CRM 데이터 추출 비서입니다. SA가 남긴 상담 전사 텍스트에서
            추천 시스템에 반영할 핵심 정보만 골라내세요.
            - keywords: 고객이 언급한 선호 브랜드/컬러/소재/무드 키워드. tagCategory는 반드시
              BRAND, COLOR, MATERIAL, MOOD 중 하나이며 keywordName은 10자 이내 단어로 작성하세요.
              언급이 없으면 빈 배열을 반환하세요.
            - restockRequested: 고객이 품절 상품의 재입고를 요청했거나 기다리고 있다는 내용이면 true,
              아니면 false.
            - restockProductName: restockRequested가 true이고 고객이 재입고를 기다리는 구체적인
              상품명(또는 상품을 특정할 수 있는 표현)을 언급했다면 그 문구를 그대로 적으세요.
              구체적인 상품을 특정할 수 없으면 null로 두세요.
            반드시 아래 JSON 형식으로만 응답하세요:
            {"keywords": [{"tagCategory": "BRAND", "keywordName": "..."}], "restockRequested": false, "restockProductName": null}
            """;

    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());
        return factory;
    }

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Override
    public RecommendationExtractionResult extract(String transcribedText) {
        if (transcribedText == null || transcribedText.isBlank() || apiKey == null || apiKey.isBlank()) {
            return new RecommendationExtractionResult(List.of(), false, null);
        }

        try {
            ChatRequest request = new ChatRequest(
                    model,
                    List.of(
                            new OpenAiDto.Message("system", SYSTEM_PROMPT),
                            new OpenAiDto.Message("user", transcribedText)
                    ),
                    0.2,
                    new ResponseFormat("json_object")
            );

            OpenAiDto.Response response = restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiDto.Response.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return new RecommendationExtractionResult(List.of(), false, null);
            }

            String content = response.choices().get(0).message().content();
            GeneratedContent parsed = objectMapper.readValue(content, GeneratedContent.class);

            return new RecommendationExtractionResult(
                    toValidKeywords(parsed.keywords()),
                    Boolean.TRUE.equals(parsed.restockRequested()),
                    sanitizeProductName(parsed.restockProductName())
            );
        } catch (Exception e) {
            log.error("AI 추천 정보 추출 중 예외 발생 (추출 결과 없음으로 처리): ", e);
            return new RecommendationExtractionResult(List.of(), false, null);
        }
    }

    private String sanitizeProductName(String rawProductName) {
        if (rawProductName == null || rawProductName.isBlank()) {
            return null;
        }
        String trimmed = rawProductName.trim();
        return trimmed.length() <= PRODUCT_NAME_MAX_LENGTH ? trimmed : trimmed.substring(0, PRODUCT_NAME_MAX_LENGTH);
    }

    private List<ExtractedKeyword> toValidKeywords(List<KeywordItem> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .filter(item -> item.keywordName() != null && !item.keywordName().isBlank())
                .filter(item -> item.keywordName().length() <= KEYWORD_NAME_MAX_LENGTH)
                .map(item -> {
                    TagCategory tagCategory = parseTagCategory(item.tagCategory());
                    return tagCategory == null ? null : new ExtractedKeyword(tagCategory, item.keywordName());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private TagCategory parseTagCategory(String rawTagCategory) {
        if (rawTagCategory == null || rawTagCategory.isBlank()) {
            return null;
        }
        try {
            return TagCategory.valueOf(rawTagCategory.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record ChatRequest(String model, List<OpenAiDto.Message> messages, Double temperature,
                                ResponseFormat response_format) {
    }

    private record ResponseFormat(String type) {
    }

    private record KeywordItem(String tagCategory, String keywordName) {
    }

    private record GeneratedContent(List<KeywordItem> keywords, Boolean restockRequested, String restockProductName) {
    }
}
