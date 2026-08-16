package com.example.suphernova.domain.briefing.client;

/**
 * 음성 → 텍스트 전사(STT) 서비스 클라이언트.
 * referenceId는 브리핑뿐 아니라 향후 "기록 작성" 등 다른 음성 인식 기능에서도
 * 콜백을 매칭하는 용도로 재사용할 수 있도록 범용으로 둔다.
 */
public interface SttClient {

    /**
     * @return 비동기 전사 작업 ID
     */
    String requestTranscription(String audioUrl, Long referenceId);
}
