package com.example.suphernova.domain.briefing.client;

/**
 * 전사 텍스트에서 추천 상태에 반영할 핵심 정보(선호 키워드, 재입고 요청 여부)만 AI로 추출한다.
 */
public interface AiRecommendationExtractionClient {

    RecommendationExtractionResult extract(String transcribedText);
}
