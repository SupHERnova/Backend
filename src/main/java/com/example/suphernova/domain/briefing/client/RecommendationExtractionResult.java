package com.example.suphernova.domain.briefing.client;

import com.example.suphernova.domain.product.entity.TagCategory;
import java.util.List;

/**
 * 전사 텍스트에서 AI가 추출한 추천 상태 반영용 핵심 정보.
 */
public record RecommendationExtractionResult(
        List<ExtractedKeyword> keywords,
        boolean restockRequested,
        String restockProductName
) {

    public record ExtractedKeyword(TagCategory tagCategory, String keywordName) {
    }
}
