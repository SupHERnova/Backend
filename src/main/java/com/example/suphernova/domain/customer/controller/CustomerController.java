package com.example.suphernova.domain.customer.controller;

import com.example.suphernova.domain.customer.dto.CustomerCreateRequest;
import com.example.suphernova.domain.customer.dto.CustomerDetailResponse;
import com.example.suphernova.domain.customer.dto.CustomerListResponse;
import com.example.suphernova.domain.customer.service.CustomerService;
import com.example.suphernova.global.apiPayload.ApiResponse;
import com.example.suphernova.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer", description = "고객 API")
@RestController
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "고객 등록", description = "매장에 새로운 고객을 등록합니다.")
    @PostMapping("/api/customers")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request
    ) {
        CustomerDetailResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.CREATED, response));
    }

    @Operation(summary = "매장별 고객 목록 및 이름 검색 조회", description = "매장에 등록된 고객 목록을 조회하거나 이름을 검색합니다. (취향 태그 포함)")
    @GetMapping("/api/stores/{storeId}/customers")
    public ResponseEntity<ApiResponse<List<CustomerListResponse>>> getCustomersByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) String search
    ) {
        List<CustomerListResponse> response = customerService.getCustomersByStore(storeId, search);
        return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.OK, response));
    }

    @Operation(summary = "고객 프로필 상세 조회", description = "고객의 누적 금액, 회원등급, 담당 SA 등 상세 프로필 정보를 조회합니다.")
    @GetMapping("/api/customers/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomer(
            @PathVariable Long customerId
    ) {
        CustomerDetailResponse response = customerService.getCustomerDetail(customerId);
        return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.of(GeneralSuccessCode.OK, response));
    }
}