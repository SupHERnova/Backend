package com.example.suphernova.domain.briefing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.entity.Keyword;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.customer.repository.KeywordRepository;
import com.example.suphernova.domain.order.entity.CustomOrder;
import com.example.suphernova.domain.order.entity.OrderItem;
import com.example.suphernova.domain.order.repository.CustomOrderRepository;
import com.example.suphernova.domain.order.repository.OrderItemRepository;
import com.example.suphernova.domain.product.entity.Product;
import com.example.suphernova.domain.product.entity.ProductKeyword;
import com.example.suphernova.domain.product.entity.TagCategory;
import com.example.suphernova.domain.product.repository.ProductKeywordRepository;
import com.example.suphernova.domain.product.repository.ProductRepository;
import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BriefingContextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductKeywordRepository productKeywordRepository;

    @Autowired
    private CustomOrderRepository customOrderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

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
    }

    @Test
    @DisplayName("선호 키워드는 태그 카테고리별로 묶어서 반환한다")
    void getBriefingContext_groupsPreferredKeywordsByCategory() throws Exception {
        keywordRepository.save(Keyword.builder().customer(customer).tagCategory(TagCategory.BRAND).keywordName("NOIR").build());
        keywordRepository.save(Keyword.builder().customer(customer).tagCategory(TagCategory.BRAND).keywordName("AUBE").build());
        keywordRepository.save(Keyword.builder().customer(customer).tagCategory(TagCategory.COLOR).keywordName("블랙").build());

        mockMvc.perform(get("/api/customers/{customerId}/briefings/context", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.customerName").value("김서윤"))
                .andExpect(jsonPath("$.result.preferredKeywords.brand").value("NOIR · AUBE"))
                .andExpect(jsonPath("$.result.preferredKeywords.color").value("블랙"))
                .andExpect(jsonPath("$.result.preferredKeywords.material").doesNotExist());
    }

    @Test
    @DisplayName("최근 구매 이력은 최신 주문순으로 상품명/컬러/가격과 함께 반환한다")
    void getBriefingContext_returnsRecentPurchasesNewestFirst() throws Exception {
        Product tote = createProduct("AUBE 미니 토트백", "블랙");
        Product belt = createProduct("NOIR 슬림 벨트", "블랙");

        CustomOrder olderOrder = customOrderRepository.save(CustomOrder.builder()
                .customer(customer)
                .createdAt(LocalDate.now().minusDays(1))
                .build());
        orderItemRepository.save(OrderItem.builder().order(olderOrder).product(belt).quantity(1).build());

        CustomOrder newerOrder = customOrderRepository.save(CustomOrder.builder()
                .customer(customer)
                .createdAt(LocalDate.now())
                .build());
        orderItemRepository.save(OrderItem.builder().order(newerOrder).product(tote).quantity(1).build());

        mockMvc.perform(get("/api/customers/{customerId}/briefings/context", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.recentPurchases.length()").value(2))
                .andExpect(jsonPath("$.result.recentPurchases[0].productName").value("AUBE 미니 토트백"))
                .andExpect(jsonPath("$.result.recentPurchases[0].color").value("블랙"))
                .andExpect(jsonPath("$.result.recentPurchases[0].price").value(890000))
                .andExpect(jsonPath("$.result.recentPurchases[1].productName").value("NOIR 슬림 벨트"));
    }

    @Test
    @DisplayName("존재하지 않는 고객이면 404를 반환한다")
    void getBriefingContext_withNonExistingCustomer_returns404() throws Exception {
        mockMvc.perform(get("/api/customers/{customerId}/briefings/context", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }

    private Product createProduct(String name, String color) {
        Product product = productRepository.save(Product.builder()
                .store(store)
                .productName(name)
                .brand("NOIR")
                .price(890000L)
                .stockQuantity(3)
                .imageUrl("https://cdn.example.com/product.png")
                .build());
        productKeywordRepository.save(ProductKeyword.builder()
                .product(product)
                .tagCategory(TagCategory.COLOR)
                .tagName(color)
                .build());
        return product;
    }
}
