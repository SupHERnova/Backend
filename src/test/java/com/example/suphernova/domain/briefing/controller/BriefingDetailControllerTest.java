package com.example.suphernova.domain.briefing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.repository.BriefingRepository;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.domain.product.entity.Product;
import com.example.suphernova.domain.product.entity.ProductKeyword;
import com.example.suphernova.domain.product.entity.TagCategory;
import com.example.suphernova.domain.product.repository.ProductKeywordRepository;
import com.example.suphernova.domain.product.repository.ProductRepository;
import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BriefingDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BriefingRepository briefingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductKeywordRepository productKeywordRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    private Store store;
    private Customer customer;

    @BeforeEach
    void setUp() {
        store = storeRepository.save(Store.builder()
                .storeName("성신여대점")
                .managerName("김수진")
                .build());
        customer = customerRepository.save(Customer.builder()
                .store(store)
                .customerName("김서윤")
                .build());
        keywordRepository.save(Keyword.builder().customer(customer).keywordName("블랙").build());
    }

    private Long createBriefing() {
        Briefing briefing = briefingRepository.save(Briefing.builder()
                .customer(customer)
                .summaryText("요약")
                .scriptText("지난번 문의하신 블랙 로퍼가 오늘 입고됐습니다.")
                .build());
        return briefing.getId();
    }

    private Product createProduct(String name, Integer stockQuantity, Instant restockedAt, String tagName) {
        Product product = productRepository.save(Product.builder()
                .store(store)
                .productName(name)
                .brand("NOIR")
                .price(890000L)
                .stockQuantity(stockQuantity)
                .restockedAt(restockedAt)
                .imageUrl("https://cdn.example.com/product.png")
                .build());
        productKeywordRepository.save(ProductKeyword.builder()
                .product(product)
                .tagCategory(TagCategory.COLOR)
                .tagName(tagName)
                .build());
        return product;
    }

    @Test
    @DisplayName("오늘 재입고된 매칭 상품은 미해결 요청 해결 정보로, 나머지 매칭 상품은 추천 상품으로 반환한다")
    void getBriefingDetail_withRestockedMatch_returnsResolvedRequestAndRecommendations() throws Exception {
        Long briefingId = createBriefing();
        Product restockedProduct = createProduct("NOIR 소프트 로퍼", 2, Instant.now(), "블랙");
        Product otherProduct = createProduct("오브 숄더 백", 5, Instant.now().minusSeconds(60L * 60 * 24 * 30), "블랙");
        createProduct("무관 상품", 3, Instant.now(), "화이트");

        mockMvc.perform(get("/api/briefings/{briefingId}/detail", briefingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.customerName").value("김서윤"))
                .andExpect(jsonPath("$.result.resolvedRequest.productId").value(restockedProduct.getId()))
                .andExpect(jsonPath("$.result.resolvedRequest.restockMessage").value("오늘 2개 재입고"))
                .andExpect(jsonPath("$.result.recommendedProducts.length()").value(1))
                .andExpect(jsonPath("$.result.recommendedProducts[0].productId").value(otherProduct.getId()))
                .andExpect(jsonPath("$.result.recommendedProducts[0].matchScore").value(100));
    }

    @Test
    @DisplayName("고객 선호 키워드가 없으면 미해결 요청과 추천 상품 없이 브리핑 기본 정보만 반환한다")
    void getBriefingDetail_withoutKeywords_returnsEmptyRecommendations() throws Exception {
        keywordRepository.deleteAll();
        Long briefingId = createBriefing();
        createProduct("NOIR 소프트 로퍼", 2, Instant.now(), "블랙");

        mockMvc.perform(get("/api/briefings/{briefingId}/detail", briefingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.resolvedRequest").doesNotExist())
                .andExpect(jsonPath("$.result.recommendedProducts.length()").value(0));
    }

    @Test
    @DisplayName("존재하지 않는 브리핑을 상세 조회하면 404를 반환한다")
    void getBriefingDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/briefings/{briefingId}/detail", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }
}
