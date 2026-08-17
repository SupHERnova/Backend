package com.example.suphernova.domain.customer.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendationType {
    RESTOCK("재입고"),
    HIGH_MATCH("고일치");

    private final String description;
}