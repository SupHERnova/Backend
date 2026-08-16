package com.example.suphernova.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Stt stt,
        Tts tts,
        Internal internal,
        Storage storage
) {

    public record Stt(String baseUrl) {
    }

    public record Tts(String baseUrl) {
    }

    public record Internal(String baseUrl, String token) {
    }

    public record Storage(String audioBasePath) {
    }
}
