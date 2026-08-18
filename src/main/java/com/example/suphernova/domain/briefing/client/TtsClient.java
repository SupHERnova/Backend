package com.example.suphernova.domain.briefing.client;

public interface TtsClient {

    /**
     * @return 합성된 오디오 바이트(mp3)
     */
    byte[] synthesize(String scriptText);
}
