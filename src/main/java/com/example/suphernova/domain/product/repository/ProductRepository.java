package com.example.suphernova.domain.product.repository;

import com.example.suphernova.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. 특정 키워드와 3개 이상 일치하는 유사 고객 수 조회 (서브쿼리로 감싸서 단일 Long 반환)
    @Query("""
        SELECT COUNT(sub.customerId)
        FROM (
            SELECT k.customer.id AS customerId
            FROM Keyword k
            WHERE k.keywordName IN :keywords
              AND k.customer.id <> :customerId
            GROUP BY k.customer.id
            HAVING COUNT(DISTINCT k.keywordName) >= 3
        ) sub
    """)
    Long countSimilarCustomers(@Param("customerId") Long customerId, @Param("keywords") List<String> keywords);

    // 2. 유사 고객군의 최근 30일 카테고리별 구매 건수 집계 (기존 유지)
    @Query("""
        SELECT p.brand AS categoryName, SUM(oi.quantity) AS purchaseCount
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
        GROUP BY p.brand
        ORDER BY purchaseCount DESC
    """)
    List<CategoryStatProjection> findSimilarCustomerPurchaseStats(
            @Param("customerId") Long customerId,
            @Param("keywords") List<String> keywords,
            @Param("since") LocalDateTime since
    );

    interface CategoryStatProjection {
        String getCategoryName();
        Long getPurchaseCount();
    }
}