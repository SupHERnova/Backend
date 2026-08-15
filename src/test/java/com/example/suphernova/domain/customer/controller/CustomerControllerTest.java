package com.example.suphernova.domain.customer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    private Long storeId;

    @BeforeEach
    void setUp() {
        Store store = storeRepository.save(Store.builder()
                .storeName("성신여대점")
                .managerName("김수진")
                .build());
        storeId = store.getId();
    }

    private Long createCustomer(String customerName) throws Exception {
        String body = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": %d, "customerName": "%s", "grade": "VIP", "gender": "여성", "age": 28}
                                """.formatted(storeId, customerName)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.result.customerId", Long.class);
    }

    @Test
    @DisplayName("고객 등록에 성공하면 201과 생성된 고객 정보를 반환한다")
    void createCustomer_success() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": %d, "customerName": "이몽희", "grade": "VIP", "gender": "여성", "age": 28}
                                """.formatted(storeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201_1"))
                .andExpect(jsonPath("$.result.customerId").isNumber())
                .andExpect(jsonPath("$.result.storeId").value(storeId))
                .andExpect(jsonPath("$.result.customerName").value("이몽희"))
                .andExpect(jsonPath("$.result.grade").value("VIP"))
                .andExpect(jsonPath("$.result.gender").value("여성"))
                .andExpect(jsonPath("$.result.age").value(28));
    }

    @Test
    @DisplayName("고객명 없이 등록하면 400과 실패 응답을 반환한다")
    void createCustomer_withoutCustomerName_returns400() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": %d, "grade": "VIP"}
                                """.formatted(storeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    @DisplayName("존재하지 않는 매장으로 등록하면 404와 실패 응답을 반환한다")
    void createCustomer_withNonExistingStore_returns404() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": 999999, "customerName": "이몽희"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }

    @Test
    @DisplayName("매장별 고객 목록을 조회하면 해당 매장의 고객만 반환한다")
    void getCustomersByStore_success() throws Exception {
        createCustomer("이몽희");
        createCustomer("박수정");

        mockMvc.perform(get("/api/stores/{storeId}/customers", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].customerName").value("이몽희"))
                .andExpect(jsonPath("$.result[1].customerName").value("박수정"));
    }

    @Test
    @DisplayName("고객 단건 조회에 성공하면 200과 고객 정보를 반환한다")
    void getCustomer_success() throws Exception {
        Long customerId = createCustomer("이몽희");

        mockMvc.perform(get("/api/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.customerId").value(customerId))
                .andExpect(jsonPath("$.result.customerName").value("이몽희"));
    }

    @Test
    @DisplayName("존재하지 않는 고객을 조회하면 404와 실패 응답을 반환한다")
    void getCustomer_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/customers/{customerId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }
}
