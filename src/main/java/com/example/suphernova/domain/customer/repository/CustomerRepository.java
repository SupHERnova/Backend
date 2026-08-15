package com.example.suphernova.domain.customer.repository;

import com.example.suphernova.domain.customer.entity.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByStoreId(Long storeId);
}
