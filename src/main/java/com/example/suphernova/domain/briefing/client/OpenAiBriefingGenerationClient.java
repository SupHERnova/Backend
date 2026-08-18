package com.example.suphernova.domain.briefing.client;

import com.example.suphernova.domain.recommendation.dto.OpenAiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 고객 컨텍스트(취향/구매이력/재입고 여부 등) 텍스트를 OpenAI Chat Completions에 전달해
 * 브리핑 요약(summaryText)과 SA가 소리 내어 읽을 스크립트(scriptText)를 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiBriefingGenerationClient implements AiBriefingGenerationClient {

    private static final String SYSTEM_PROMPT = """
            당신은 명품 매장의 AI 브리핑 작성 비서입니다. SA가 고객을 만나기 전에 참고할 컨텍스트가 주어지면,
            핵심을 정리한 요약(summaryText)과 SA가 실제로 소리 내어 읽을 안내 멘트(scriptText)를 작성하세요.
            scriptText는 TTS 음성으로 재생되므로, 인사말 한 줄로 끝내지 말고 취향/최근 구매/재입고 여부 등
            주어진 정보를 자연스럽게 풀어 6~8문장 이상 충분히 상세하게 작성하세요.
            언급된 고객 취향 키워드가 있다면 keywords 배열에 담으세요.
            반드시 아래 JSON 형식으로만 응답하세요:
            {"summaryText": "요약 문장", "scriptText": "6~8문장 이상의 상세한 안내 멘트", "keywords": ["키워드", ...]}
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
    public AiBriefingGenerationResult generate(String contextText) {
        if (contextText == null || contextText.isBlank()) {
            return new AiBriefingGenerationResult(null, null, List.of());
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API Key가 설정되지 않아 원문을 그대로 브리핑 스크립트로 사용합니다.");
            return new AiBriefingGenerationResult(contextText, contextText, List.of());
        }

        try {
            ChatRequest request = new ChatRequest(
                    model,
                    List.of(
                            new OpenAiDto.Message("system", SYSTEM_PROMPT),
                            new OpenAiDto.Message("user", contextText)
                    ),
                    0.5,
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
                return new AiBriefingGenerationResult(contextText, contextText, List.of());
            }

            String content = response.choices().get(0).message().content();
            GeneratedContent parsed = objectMapper.readValue(content, GeneratedContent.class);

            return new AiBriefingGenerationResult(
                    parsed.summaryText(),
                    parsed.scriptText(),
                    parsed.keywords() == null ? List.of() : parsed.keywords()
            );
        } catch (Exception e) {
            log.error("AI 브리핑 생성 중 예외 발생 (Fallback 원문 사용): ", e);
            return new AiBriefingGenerationResult(contextText, contextText, List.of());
        }
    }

    private record ChatRequest(String model, List<OpenAiDto.Message> messages, Double temperature,
                                ResponseFormat response_format) {
    }

    private record ResponseFormat(String type) {
    }

    private record GeneratedContent(String summaryText, String scriptText, List<String> keywords) {
    }
}
