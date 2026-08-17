package com.example.suphernova.domain.customer.repository;

import com.example.suphernova.domain.customer.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 전체 고객 목록 (최근 방문일 순, id 내림차순 보조 정렬, 페이징)
    Slice<Customer> findAllByStoreIdOrderByLastVisitAtDescIdDesc(Long storeId, Pageable pageable);

    // 고객 이름 검색 (대소문자 구분 없음 + 최근 방문일 순, id 내림차순 보조 정렬, 페이징)
    Slice<Customer> findAllByStoreIdAndCustomerNameContainingIgnoreCaseOrderByLastVisitAtDescIdDesc(
            Long storeId, String customerName, Pageable pageable
    );
}