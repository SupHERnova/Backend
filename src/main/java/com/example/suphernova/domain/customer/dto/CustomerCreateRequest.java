package com.example.suphernova.domain.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(

        @NotNull(message = "매장 ID는 필수입니다.")
        Long storeId,

        @NotBlank(message = "고객명은 필수입니다.")
        @Size(max = 50, message = "고객명은 50자 이하여야 합니다.")
        String customerName,

        @Size(max = 20, message = "등급은 20자 이하여야 합니다.")
        String grade,

        @Size(max = 10, message = "성별은 10자 이하여야 합니다.")
        String gender,

        @PositiveOrZero(message = "나이는 0 이상이어야 합니다.")
        Integer age
) {
}
