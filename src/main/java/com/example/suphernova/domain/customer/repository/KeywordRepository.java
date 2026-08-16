package com.example.suphernova.domain.customer.repository;

import com.example.suphernova.domain.customer.entity.Keyword;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    List<Keyword> findAllByCustomerId(Long customerId);
}
