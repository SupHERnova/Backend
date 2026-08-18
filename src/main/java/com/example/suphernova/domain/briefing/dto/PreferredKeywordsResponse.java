package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.product.entity.TagCategory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record PreferredKeywordsResponse(
        String brand,
        String color,
        String material,
        String mood
) {

    public static PreferredKeywordsResponse from(List<Keyword> keywords) {
        Map<TagCategory, String> byCategory = keywords.stream()
                .filter(k -> k.getTagCategory() != null && k.getKeywordName() != null)
                .collect(Collectors.groupingBy(
                        Keyword::getTagCategory,
                        Collectors.mapping(Keyword::getKeywordName, Collectors.joining(" · "))
                ));

        return new PreferredKeywordsResponse(
                byCategory.get(TagCategory.BRAND),
                byCategory.get(TagCategory.COLOR),
                byCategory.get(TagCategory.MATERIAL),
                byCategory.get(TagCategory.MOOD)
        );
    }
}
