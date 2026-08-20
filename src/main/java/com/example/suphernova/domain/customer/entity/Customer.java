package com.example.suphernova.domain.customer.entity;

import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Customer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "customer_name", nullable = false, length = 50)
    private String customerName;

    @Column(name = "grade", length = 20)
    private String grade;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "last_visit_at")
    private Instant lastVisitAt;

    @Column(name = "crm_id")
    private String crmId;

    @Column(name = "total_purchase_count")
    private Integer totalPurchaseCount;

    @Column(name = "total_purchase_amount")
    private Long totalPurchaseAmount;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "sa_name", length = 50)
    private String saName;

    @Column(name = "preferred_contact_method", length = 100)
    private String preferredContactMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", length = 20)
    private RecommendationType recommendationType;

    @Column(name = "restock_requested_product_name", length = 100)
    private String restockRequestedProductName;

    public void updateRecommendationType(RecommendationType recommendationType) {
        this.recommendationType = recommendationType;
    }

    /**
     * 고객이 재입고를 요청한 특정 상품명을 함께 기록한다. 상품명을 특정하지 못한 경우(null)에는
     * 재입고 요청 여부만 반영되고, 실제 매칭은 취향 키워드 기반으로 대체된다.
     */
    public void markRestockRequested(String requestedProductName) {
        this.recommendationType = RecommendationType.RESTOCK;
        this.restockRequestedProductName = requestedProductName;
    }
}
