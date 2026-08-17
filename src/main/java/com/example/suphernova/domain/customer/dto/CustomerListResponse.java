package com.example.suphernova.domain.customer.dto;

import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.RecommendationType;
import java.time.Instant;
import java.util.List;

public record CustomerListResponse(
        Long customerId,
        String customerName,
        String grade,
        String gender,
        Integer age,
        Instant lastVisitAt,
        RecommendationType recommendationType,
        List<String> keywords
) {
    public static CustomerListResponse of(Customer customer, List<String> keywords) {
        return new CustomerListResponse(
                customer.getId(),
                customer.getCustomerName(),
                customer.getGrade(),
                customer.getGender(),
                customer.getAge(),
                customer.getLastVisitAt(),
                customer.getRecommendationType(),
                keywords
        );
    }
}