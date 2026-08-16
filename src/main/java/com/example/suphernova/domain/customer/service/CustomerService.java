package com.example.suphernova.domain.customer.service;

import com.example.suphernova.domain.customer.dto.CustomerCreateRequest;
import com.example.suphernova.domain.customer.dto.CustomerResponse;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        Customer customer = Customer.builder()
                .store(store)
                .customerName(request.customerName())
                .grade(request.grade())
                .gender(request.gender())
                .age(request.age())
                .build();

        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByStore(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new ProjectException(GeneralErrorCode.NOT_FOUND);
        }

        return customerRepository.findAllByStoreId(storeId).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
    }
}
