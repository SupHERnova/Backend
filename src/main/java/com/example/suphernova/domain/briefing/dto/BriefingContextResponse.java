package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.recommendation.dto.RecommendationResponse;
import java.util.List;

public record BriefingContextResponse(
        Long briefingId,
        Long customerId,
        String customerName,
        PreferredKeywordsResponse preferredKeywords,
        List<RecentPurchaseResponse> recentPurchases,
        RecommendationResponse.SimilarCustomerStatsDto similarCustomerStats,
        UnresolvedRequestResponse unresolvedRequest
) {
}
