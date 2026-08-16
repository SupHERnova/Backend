package com.example.suphernova.domain.record.repository;

import com.example.suphernova.domain.record.entity.Records;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecordRepository extends JpaRepository<Records, Long> {
    List<Records> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);}