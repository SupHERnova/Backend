package com.example.suphernova.domain.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreCreateRequest(

        @NotBlank(message = "매장명은 필수입니다.")
        @Size(max = 100, message = "매장명은 100자 이하여야 합니다.")
        String storeName,

        @Size(max = 100, message = "담당자명은 100자 이하여야 합니다.")
        String managerName
) {
}
