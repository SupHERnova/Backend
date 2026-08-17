package com.example.suphernova.domain.customer.service;

import com.example.suphernova.domain.customer.dto.CustomerCreateRequest;
import com.example.suphernova.domain.customer.dto.CustomerDetailResponse;
import com.example.suphernova.domain.customer.dto.CustomerListResponse;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final KeywordRepository keywordRepository;

    /**
     * 신규 고객 등록
     */
    @Transactional
    public CustomerDetailResponse createCustomer(CustomerCreateRequest request) {
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        Customer customer = Customer.builder()
                .store(store)
                .customerName(request.customerName())
                .grade(request.grade())
                .gender(request.gender())
                .age(request.age())
                .build();

        return CustomerDetailResponse.from(customerRepository.save(customer));
    }

    /**
     * 매장별 고객 목록 조회 및 이름 검색 (취향 키워드 태그 목록 포함)
     * - 최근 방문일(lastVisitAt) 내림차순 및 id 내림차순 보조 정렬 & Pageable 페이징 적용
     */
    @Transactional(readOnly = true)
    public Slice<CustomerListResponse> getCustomersByStore(Long storeId, String search, Pageable pageable) {
        if (!storeRepository.existsById(storeId)) {
            throw new ProjectException(GeneralErrorCode.NOT_FOUND);
        }

        Slice<Customer> customers;
        if (search == null || search.trim().isEmpty()) {
            // 보조 정렬 조건(IdDesc) 추가된 Repository 메서드로 변경
            customers = customerRepository.findAllByStoreIdOrderByLastVisitAtDescIdDesc(storeId, pageable);
        } else {
            // 보조 정렬 조건(IdDesc) 추가된 Repository 메서드로 변경
            customers = customerRepository.findAllByStoreIdAndCustomerNameContainingIgnoreCaseOrderByLastVisitAtDescIdDesc(
                    storeId, search.trim(), pageable
            );
        }

        List<Long> customerIds = customers.stream().map(Customer::getId).toList();

        // N+1 쿼리 방지: 현재 페이지 대상 취향 키워드 일괄 조회
        Map<Long, List<String>> keywordMap = keywordRepository.findAllByCustomerIdIn(customerIds)
                .stream()
                .collect(Collectors.groupingBy(
                        k -> k.getCustomer().getId(),
                        Collectors.mapping(Keyword::getKeywordName, Collectors.toList())
                ));

        return customers.map(customer ->
                CustomerListResponse.of(customer, keywordMap.getOrDefault(customer.getId(), List.of()))
        );
    }

    /**
     * 고객 상세 프로필 정보 조회
     */
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(Long customerId) {
        return customerRepository.findById(customerId)
                .map(CustomerDetailResponse::from)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
    }
}