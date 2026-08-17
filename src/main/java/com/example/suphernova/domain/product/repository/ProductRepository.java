package com.example.suphernova.domain.product.repository;

import com.example.suphernova.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. 특정 키워드와 3개 이상 일치하는 유사 고객 수 조회 (5명 미만 체크용)
    @Query("""
        SELECT COUNT(DISTINCT k.customer.id)
        FROM Keyword k
        WHERE k.keywordName IN :keywords AND k.customer.id <> :targetCustomerId
        AND k.customer.id IN (
            SELECT k2.customer.id
            FROM Keyword k2
            WHERE k2.keywordName IN :keywords AND k2.customer.id <> :targetCustomerId
            GROUP BY k2.customer.id
            HAVING COUNT(DISTINCT k2.keywordName) >= 3
        )
    """)
    Long countSimilarCustomers(@Param("targetCustomerId") Long targetCustomerId, @Param("keywords") List<String> keywords);

    // 2. 유사 고객군(서로 다른 키워드 3개 이상 일치)의 최근 30일 카테고리별 구매 건수 집계
    @Query("""
        SELECT p.brand AS categoryName, SUM(oi.quantity) AS purchaseCount
        FROM OrderItem oi
        JOIN oi.order o
        JOIN oi.product p
        WHERE o.customer.id IN (
            SELECT k.customer.id
            FROM Keyword k
            WHERE k.keywordName IN :keywords AND k.customer.id <> :targetCustomerId
            GROUP BY k.customer.id
            HAVING COUNT(DISTINCT k.keywordName) >= 3
        )
        AND o.createdAt >= :since
        GROUP BY p.brand
        ORDER BY purchaseCount DESC
    """)
    List<CategoryStatProjection> findSimilarCustomerPurchaseStats(
            @Param("targetCustomerId") Long targetCustomerId,
            @Param("keywords") List<String> keywords,
            @Param("since") LocalDateTime since
    );

    interface CategoryStatProjection {
        String getCategoryName();
        Long getPurchaseCount();
    }
}