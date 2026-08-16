package com.example.suphernova.domain.briefing.client;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * TODO: 실제 AI 정보 추출/스크립트 생성 서비스로 교체 필요.
 * 지금은 전사 텍스트를 그대로 스크립트로 통과시키고 키워드는 추출하지 않는
 * 임시 구현으로, 전체 파이프라인이 end-to-end로 동작하는지 확인하는 용도다.
 */
@Component
public class StubAiBriefingGenerationClient implements AiBriefingGenerationClient {

    @Override
    public AiBriefingGenerationResult generate(String transcribedText) {
        if (transcribedText == null || transcribedText.isBlank()) {
            return new AiBriefingGenerationResult(null, null, List.of());
        }

        return new AiBriefingGenerationResult(transcribedText, transcribedText, List.of());
    }
}
