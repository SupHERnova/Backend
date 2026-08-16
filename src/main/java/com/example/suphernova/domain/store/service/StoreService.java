package com.example.suphernova.domain.store.service;

import com.example.suphernova.domain.store.dto.StoreCreateRequest;
import com.example.suphernova.domain.store.dto.StoreResponse;
import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        Store store = Store.builder()
                .storeName(request.storeName())
                .managerName(request.managerName())
                .build();

        return StoreResponse.from(storeRepository.save(store));
    }
}
