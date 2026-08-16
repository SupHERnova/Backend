package com.example.suphernova.domain.recommendation.controller;

import com.example.suphernova.domain.recommendation.dto.RecommendationResponse;
import com.example.suphernova.domain.recommendation.service.RecommendationService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendation", description = "AI 상품 추천 API")
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "고객별 AI 추천 상품 목록 조회", description = "특정 고객의 취향 키워드를 기반으로 맞춤 상품 목록을 추천합니다.")
    @GetMapping("/api/customers/{customerId}/recommendations")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendedProducts(
            @PathVariable Long customerId
    ) {
        RecommendationResponse response = recommendationService.getRecommendedProducts(customerId);
        return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.OK, response));
    }
}