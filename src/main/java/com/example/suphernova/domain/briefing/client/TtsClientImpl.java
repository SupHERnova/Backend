package com.example.suphernova.domain.briefing.client;

import com.example.suphernova.domain.briefing.client.dto.TtsSynthesizeRequest;
import com.example.suphernova.domain.briefing.client.dto.TtsSynthesizeResponse;
import com.example.suphernova.global.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TtsClientImpl implements TtsClient {

    private final AppProperties appProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public String requestSynthesis(String scriptText, Long referenceId) {
        TtsSynthesizeRequest request = new TtsSynthesizeRequest(
                scriptText,
                appProperties.internal().baseUrl() + "/internal/tts-callback",
                referenceId
        );

        TtsSynthesizeResponse response = restClient.post()
                .uri(appProperties.tts().baseUrl() + "/synthesize")
                .body(request)
                .retrieve()
                .body(TtsSynthesizeResponse.class);

        return response.jobId();
    }
}
