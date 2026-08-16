package com.example.suphernova.domain.order.repository;

import com.example.suphernova.domain.order.entity.CustomOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {
}
