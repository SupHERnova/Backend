package com.example.suphernova.domain.recommendation.service;

import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.entity.RecommendationType;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.domain.product.entity.Product;
import com.example.suphernova.domain.product.entity.ProductKeyword;
import com.example.suphernova.domain.product.repository.ProductRepository;
import com.example.suphernova.domain.recommendation.dto.OpenAiDto;
import com.example.suphernova.domain.recommendation.dto.RecommendationResponse;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CustomerRepository customerRepository;
    private final KeywordRepository keywordRepository;
    private final ProductRepository productRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendedProducts(Long customerId) {
        // 1. API 키 검증
        if (openAiApiKey == null || openAiApiKey.isBlank() || "mock-key".equals(openAiApiKey)) {
            log.warn("OpenAI API Key가 설정되지 않았습니다.");
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
        }

        // 2. 고객 및 취향 키워드 조회
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        List<String> customerKeywords = keywordRepository.findAllByCustomerId(customerId)
                .stream()
                .map(Keyword::getKeywordName)
                .toList();

        List<Product> allProducts = productRepository.findAll();

        // 3. [슬롯 1] 재입고 / 고일치 조건 분기 및 Top 1
        RecommendationResponse.RestockedProductDto restockedProductDto = getTopSlotProduct(customer, allProducts, customerKeywords);

        // 4. [슬롯 2] 취향 매칭 추천 상품 Top 3 (고객-상품 키워드 일치율)
        List<RecommendationResponse.MatchedProductDto> matchedProducts = getTopMatchedProducts(allProducts, customerKeywords);

        // 5. [슬롯 3] 유사 취향 고객 데이터 (5명 미만 시 null 반환)
        RecommendationResponse.SimilarCustomerStatsDto similarCustomerStats = getSimilarCustomerStats(customerId, customerKeywords);

        // 6. [슬롯 4] AI 추천 첫 멘트 생성
        String recommendationComment = fetchLlmMatchReason(customer, customerKeywords, restockedProductDto, matchedProducts);

        return new RecommendationResponse(
                restockedProductDto,
                matchedProducts,
                similarCustomerStats,
                recommendationComment
        );
    }

    private RecommendationResponse.RestockedProductDto getTopSlotProduct(Customer customer, List<Product> products, List<String> keywords) {
        boolean isRestockType = customer.getRecommendationType() == RecommendationType.RESTOCK;

        return products.stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                .filter(p -> {
                    // RESTOCK 타입: 재입고 일자가 존재하는 상품
                    if (isRestockType) {
                        return p.getRestockedAt() != null;
                    }
                    // HIGH_MATCH 타입: 취향 일치율이 0% 초과인 상품
                    return calculateMatchRate(p, keywords) > 0;
                })
                .max(Comparator.comparingInt(p -> calculateMatchRate(p, keywords)))
                .map(p -> new RecommendationResponse.RestockedProductDto(
                        p.getId(),
                        p.getProductName(),
                        "FREE", // 필요 시 p.getSize() 등 연동
                        p.getStockQuantity(),
                        isRestockType ? "이전 요청 상품 재입고" : "최고 취향 일치 상품"
                ))
                .orElse(null);
    }

    private List<RecommendationResponse.MatchedProductDto> getTopMatchedProducts(List<Product> products, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return List.of();

        return products.stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p, calculateMatchRate(p, keywords)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .map(entry -> new RecommendationResponse.MatchedProductDto(
                        entry.getKey().getId(),
                        entry.getKey().getProductName(),
                        entry.getKey().getPrice(),
                        entry.getKey().getImageUrl(),
                        entry.getValue()
                ))
                .toList();
    }

    private RecommendationResponse.SimilarCustomerStatsDto getSimilarCustomerStats(Long customerId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;

        Long similarCount = productRepository.countSimilarCustomers(customerId, keywords);
        if (similarCount == null || similarCount < 5) {
            return null; // 프론트 예외 UI 처리
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        var stats = productRepository.findSimilarCustomerPurchaseStats(customerId, keywords, thirtyDaysAgo);

        if (stats == null || stats.isEmpty()) return null;

        long total = stats.stream().mapToLong(ProductRepository.CategoryStatProjection::getPurchaseCount).sum();
        List<RecommendationResponse.CategoryPurchaseRatioDto> ratios = stats.stream()
                .map(s -> new RecommendationResponse.CategoryPurchaseRatioDto(
                        s.getCategoryName(),
                        (int) Math.round(((double) s.getPurchaseCount() / (total == 0 ? 1 : total)) * 100)
                ))
                .limit(2)
                .toList();

        String topCategory = ratios.isEmpty() ? "인기 상품" : ratios.get(0).categoryName();
        return new RecommendationResponse.SimilarCustomerStatsDto(topCategory, ratios);
    }

    private int calculateMatchRate(Product product, List<String> customerKeywords) {
        if (customerKeywords == null || customerKeywords.isEmpty() || product.getProductKeywords() == null) {
            return 0;
        }

        List<String> productTagNames = product.getProductKeywords().stream()
                .map(ProductKeyword::getTagName)
                .toList();

        long matchCount = customerKeywords.stream()
                .filter(productTagNames::contains)
                .count();

        return (int) Math.round(((double) matchCount / customerKeywords.size()) * 100);
    }

    private String fetchLlmMatchReason(Customer customer, List<String> keywords,
                                       RecommendationResponse.RestockedProductDto restocked,
                                       List<RecommendationResponse.MatchedProductDto> matched) {
        try {
            String matchedNames = (matched == null || matched.isEmpty()) ? "없음" :
                    matched.stream().map(RecommendationResponse.MatchedProductDto::productName).collect(Collectors.joining(", "));
            String restockedName = (restocked != null && restocked.productName() != null) ? restocked.productName() : "없음";
            String keywordStr = (keywords == null || keywords.isEmpty()) ? "없음" : String.join(", ", keywords);

            String systemPrompt = "당신은 명품 브랜드의 인공지능 큐레이터입니다. 추천 상품과 재입고 상품 정보를 바탕으로 고객에게 건넬 고급스러운 2문장의 첫 인사 멘트를 해요체로 작성하세요.";
            String userPrompt = String.format("고객명: %s, 취향키워드: %s, 재입고/주요상품: %s, 추천상품: %s",
                    customer.getCustomerName(), keywordStr, restockedName, matchedNames);

            OpenAiDto.Request requestBody = new OpenAiDto.Request(
                    model,
                    List.of(
                            new OpenAiDto.Message("system", systemPrompt),
                            new OpenAiDto.Message("user", userPrompt)
                    ),
                    0.7
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            HttpEntity<OpenAiDto.Request> entity = new HttpEntity<>(requestBody, headers);
            OpenAiDto.Response response = restTemplate.postForObject(openAiApiUrl, entity, OpenAiDto.Response.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content().trim();
            }
            return "고객님의 취향에 맞는 맞춤형 추천 상품을 확인해보세요.";
        } catch (Exception e) {
            log.error("OpenAI 멘트 생성 중 예외 발생: ", e);
            return "고객님의 취향에 맞는 맞춤형 추천 상품을 확인해보세요.";
        }
    }
}