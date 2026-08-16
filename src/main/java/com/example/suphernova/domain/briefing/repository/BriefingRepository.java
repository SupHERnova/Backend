package com.example.suphernova.domain.briefing.repository;

import com.example.suphernova.domain.briefing.entity.Briefing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BriefingRepository extends JpaRepository<Briefing, Long> {

    List<Briefing> findAllByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);
}
