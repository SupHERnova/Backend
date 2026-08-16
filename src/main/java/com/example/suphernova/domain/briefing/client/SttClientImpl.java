package com.example.suphernova.domain.briefing.client;

import com.example.suphernova.domain.briefing.client.dto.SttTranscribeRequest;
import com.example.suphernova.domain.briefing.client.dto.SttTranscribeResponse;
import com.example.suphernova.global.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SttClientImpl implements SttClient {

    private final AppProperties appProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public String requestTranscription(String audioUrl, Long referenceId) {
        SttTranscribeRequest request = new SttTranscribeRequest(
                audioUrl,
                appProperties.internal().baseUrl() + "/internal/stt-callback",
                referenceId
        );

        SttTranscribeResponse response = restClient.post()
                .uri(appProperties.stt().baseUrl() + "/transcribe")
                .body(request)
                .retrieve()
                .body(SttTranscribeResponse.class);

        return response.jobId();
    }
}
