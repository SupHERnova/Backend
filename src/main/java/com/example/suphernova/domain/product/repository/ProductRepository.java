package com.example.suphernova.domain.product.repository;

import com.example.suphernova.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByStoreId(Long storeId);

    // 1. Native Query를 사용하여 FROM절 서브쿼리 지원 및 정확한 단일 수치 반환
    @Query(value = """
        SELECT COUNT(sub.customer_id)
        FROM (
            SELECT k.customer_id
            FROM keyword k
            WHERE k.keyword_name IN :keywords
              AND k.customer_id <> :customerId
            GROUP BY k.customer_id
            HAVING COUNT(DISTINCT k.keyword_name) >= 3
        ) sub
    """, nativeQuery = true)
    Long countSimilarCustomers(@Param("customerId") Long customerId, @Param("keywords") List<String> keywords);

    // 2. 유사 고객군의 최근 30일 카테고리별 구매 건수 집계 (CustomOrder의 createdAt 타입인 LocalDate로 매칭)
    @Query("""
        SELECT p.productName AS productName, SUM(oi.quantity) AS purchaseCount
        FROM OrderItem oi
        JOIN oi.order o
        JOIN oi.product p
        WHERE o.customer.id IN (
            SELECT k.customer.id
            FROM Keyword k
            WHERE k.keywordName IN :keywords
              AND k.customer.id <> :customerId
            GROUP BY k.customer.id
            HAVING COUNT(DISTINCT k.keywordName) >= 3
        )
        AND o.createdAt >= :since
        GROUP BY p.productName
        ORDER BY purchaseCount DESC
    """)
    List<ProductStatProjection> findSimilarCustomerPurchaseStats(
            @Param("customerId") Long customerId,
            @Param("keywords") List<String> keywords,
            @Param("since") LocalDate since
    );

    interface ProductStatProjection {
        String getProductName();
        Long getPurchaseCount();
    }
}