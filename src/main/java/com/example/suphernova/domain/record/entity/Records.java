package com.example.suphernova.domain.record.entity;

import com.example.suphernova.domain.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Records {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "raw_note", nullable = false, columnDefinition = "TEXT")
    private String rawNote;

    @Column(name = "ai_summary", nullable = false, columnDefinition = "TEXT")
    private String aiSummary;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Records (Customer customer, String rawNote, String aiSummary) {
        this.customer = customer;
        this.rawNote = rawNote;
        this.aiSummary = aiSummary;
    }
}