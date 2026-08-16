package com.example.suphernova.domain.customer.repository;

import com.example.suphernova.domain.customer.entity.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 전체 고객 목록 (최근 방문일 순)
    List<Customer> findAllByStoreIdOrderByLastVisitAtDesc(Long storeId);

    // 고객 이름 검색 (대소문자 구분 없음 + 최근 방문일 순)
    List<Customer> findAllByStoreIdAndCustomerNameContainingIgnoreCaseOrderByLastVisitAtDesc(Long storeId, String customerName);
}