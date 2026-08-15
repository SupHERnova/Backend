package com.example.suphernova.domain.store.controller;

import com.example.suphernova.domain.store.dto.StoreCreateRequest;
import com.example.suphernova.domain.store.dto.StoreResponse;
import com.example.suphernova.domain.store.service.StoreService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Store", description = "매장 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "매장 등록", description = "새로운 매장을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreCreateRequest request
    ) {
        StoreResponse response = storeService.createStore(request);
        return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.CREATED, response));
    }
}
