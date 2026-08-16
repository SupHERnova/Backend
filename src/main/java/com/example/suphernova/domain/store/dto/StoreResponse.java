package com.example.suphernova.domain.store.dto;

import com.example.suphernova.domain.store.entity.Store;

public record StoreResponse(
        Long storeId,
        String storeName,
        String managerName
) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getStoreName(), store.getManagerName());
    }
}
