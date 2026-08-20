package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.client.AiRecommendationExtractionClient;
import com.example.suphernova.domain.briefing.client.RecommendationExtractionResult;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전사 텍스트에서 AI로 추출한 선호 키워드/재입고 요청 여부를 추천 상태
 * (Keyword, Customer.recommendationType)에 반영한다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationReflectionService {

    private final KeywordRepository keywordRepository;
    private final AiRecommendationExtractionClient aiRecommendationExtractionClient;

    @Transactional
    public void reflect(Customer customer, String transcribedText) {
        RecommendationExtractionResult extraction = aiRecommendationExtractionClient.extract(transcribedText);

        extraction.keywords().forEach(keyword -> keywordRepository.save(
                Keyword.builder()
                        .customer(customer)
                        .tagCategory(keyword.tagCategory())
                        .keywordName(keyword.keywordName())
                        .build()
        ));

        if (extraction.restockRequested()) {
            customer.markRestockRequested(extraction.restockProductName());
        }
    }
}
