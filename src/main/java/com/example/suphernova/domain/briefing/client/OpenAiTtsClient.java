package com.example.suphernova.domain.briefing.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiTtsClient implements TtsClient {

    private static final String SPEECH_URL = "https://api.openai.com/v1/audio/speech";

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

    @Value("${openai.tts.model:tts-1}")
    private String model;

    @Value("${openai.tts.voice:alloy}")
    private String voice;

    @Value("${openai.tts.speed:1.15}")
    private double speed;

    @Override
    public byte[] synthesize(String scriptText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RestClientException("OpenAI API 키가 설정되지 않았습니다.");
        }

        return restClient.post()
                .uri(SPEECH_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SpeechRequest(model, voice, scriptText, "mp3", speed))
                .retrieve()
                .body(byte[].class);
    }

    private record SpeechRequest(String model, String voice, String input, String response_format, double speed) {
    }
}
