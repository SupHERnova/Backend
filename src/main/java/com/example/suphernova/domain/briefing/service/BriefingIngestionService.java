package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SA 앱이 전사한 텍스트를 받아 (1) AI로 추천 상태(선호 키워드/재입고 요청)에 반영하고,
 * (2) 그 반영된 추천 상태를 바탕으로 브리핑 생성을 호출하는 파이프라인의 진입점.
 * 컨트롤러에서 호출되는 유일한 지점이며, 브리핑 생성 자체는 {@link BriefingGenerationService}에
 * 위임한다(별도 API로 외부에 노출하지 않는 백엔드 내부 전용 호출).
 */
@Service
@RequiredArgsConstructor
public class BriefingIngestionService {

    private final CustomerRepository customerRepository;
    private final RecommendationReflectionService recommendationReflectionService;
    private final BriefingGenerationService briefingGenerationService;

    @Transactional
    public BriefingResponse ingestText(Long customerId, String transcribedText) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        recommendationReflectionService.reflect(customer, transcribedText);

        return briefingGenerationService.generateFromRecommendation(customerId);
    }
}
