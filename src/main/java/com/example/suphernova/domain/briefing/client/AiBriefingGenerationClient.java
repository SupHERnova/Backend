package com.example.suphernova.domain.briefing.client;

/**
 * 전사된 텍스트에서 유효 정보(선호 키워드 등)를 추출하고 브리핑 스크립트를 생성한다.
 * 실제 AI 로직은 이번 작업 범위 밖이라, 다른 외부 서비스로 교체될 것을 전제로
 * STT/TTS 클라이언트와 동일한 형태의 인터페이스만 정의해둔다.
 * (docs/STT-TTS-INTEGRATION.md 참고)
 */
public interface AiBriefingGenerationClient {

    AiBriefingGenerationResult generate(String transcribedText);
}
