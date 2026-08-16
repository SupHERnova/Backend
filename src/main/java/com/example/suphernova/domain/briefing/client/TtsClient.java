package com.example.suphernova.domain.briefing.client;

public interface TtsClient {

    /**
     * @return 비동기 음성 합성 작업 ID
     */
    String requestSynthesis(String scriptText, Long referenceId);
}
