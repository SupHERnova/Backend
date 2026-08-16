package com.example.suphernova.domain.recommendation.dto;

import java.util.List;

public class OpenAiDto {

    public record Request(
            String model,
            List<Message> messages,
            Double temperature
    ) {}

    public record Message(
            String role,
            String content
    ) {}

    public record Response(
            List<Choice> choices
    ) {}

    public record Choice(
            Message message
    ) {}
}