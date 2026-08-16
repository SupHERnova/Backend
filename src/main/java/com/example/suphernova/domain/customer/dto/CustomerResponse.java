package com.example.suphernova.domain.customer.dto;

import com.example.suphernova.domain.customer.entity.Customer;
import java.time.Instant;

public record CustomerResponse(
        Long customerId,
        Long storeId,
        String customerName,
        String grade,
        String gender,
        Integer age,
        Instant lastVisitAt
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getStore().getId(),
                customer.getCustomerName(),
                customer.getGrade(),
                customer.getGender(),
                customer.getAge(),
                customer.getLastVisitAt()
        );
    }
}
