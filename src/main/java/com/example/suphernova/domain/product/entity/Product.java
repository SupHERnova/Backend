package com.example.suphernova.domain.product.entity;

import com.example.suphernova.domain.store.entity.Store;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "brand", length = 50)
    private String brand;

    @Column(name = "price")
    private Long price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "restocked_at")
    private Instant restockedAt;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductKeyword> productKeywords = new ArrayList<>();
}
