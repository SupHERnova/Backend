package com.example.suphernova.domain.customer.dto;

import com.example.suphernova.domain.customer.entity.Customer;
import java.time.Instant;

public record CustomerDetailResponse(
        Long customerId,
        String crmId,
        String customerName,
        String grade,
        Integer totalPurchaseCount,
        Long totalPurchaseAmount,
        Instant lastVisitAt,
        String phone,
        String saName,
        String preferredContactMethod
) {
    public static CustomerDetailResponse from(Customer customer) {
        return new CustomerDetailResponse(
                customer.getId(),
                customer.getCrmId(),
                customer.getCustomerName(),
                customer.getGrade(),
                customer.getTotalPurchaseCount(),
                customer.getTotalPurchaseAmount(),
                customer.getLastVisitAt(),
                customer.getPhone(),
                customer.getSaName(),
                customer.getPreferredContactMethod()
        );
    }
}