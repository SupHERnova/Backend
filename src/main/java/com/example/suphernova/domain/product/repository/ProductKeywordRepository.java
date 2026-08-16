package com.example.suphernova.domain.product.repository;

import com.example.suphernova.domain.product.entity.ProductKeyword;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductKeywordRepository extends JpaRepository<ProductKeyword, Long> {

    List<ProductKeyword> findAllByProductId(Long productId);
}
