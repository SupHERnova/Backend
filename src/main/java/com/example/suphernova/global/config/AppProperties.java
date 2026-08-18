package com.example.suphernova.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Storage storage
) {

    public record Storage(String audioBasePath) {
    }
}
