package com.example.suphernova.domain.briefing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
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
class BriefingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Long customerId;

    @BeforeEach
    void setUp() {
        Store store = storeRepository.save(Store.builder()
                .storeName("성신여대점")
                .managerName("김수진")
                .build());
        Customer customer = customerRepository.save(Customer.builder()
                .store(store)
                .customerName("이몽희")
                .build());
        customerId = customer.getId();
    }

    private Long createBriefing(String summaryText) throws Exception {
        String body = mockMvc.perform(post("/api/briefings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": %d, "summaryText": "%s", "scriptText": "안녕하세요"}
                                """.formatted(customerId, summaryText)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.result.briefingId", Long.class);
    }

    @Test
    @DisplayName("브리핑 등록에 성공하면 201과 생성된 브리핑 정보를 반환한다")
    void createBriefing_success() throws Exception {
        mockMvc.perform(post("/api/briefings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": %d, "summaryText": "VIP 고객, 어깨 통증 호소",
                                 "scriptText": "이몽희님은 지난 방문에서...",
                                 "ttsAudioUrl": "https://cdn.example.com/tts/1.mp3", "ttsDuration": 42}
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201_1"))
                .andExpect(jsonPath("$.result.briefingId").isNumber())
                .andExpect(jsonPath("$.result.customerId").value(customerId))
                .andExpect(jsonPath("$.result.summaryText").value("VIP 고객, 어깨 통증 호소"))
                .andExpect(jsonPath("$.result.scriptText").value("이몽희님은 지난 방문에서..."))
                .andExpect(jsonPath("$.result.ttsAudioUrl").value("https://cdn.example.com/tts/1.mp3"))
                .andExpect(jsonPath("$.result.ttsDuration").value(42))
                .andExpect(jsonPath("$.result.status").value("SUCCESS"))
                .andExpect(jsonPath("$.result.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("customerId 없이 등록하면 400과 실패 응답을 반환한다")
    void createBriefing_withoutCustomerId_returns400() throws Exception {
        mockMvc.perform(post("/api/briefings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"summaryText": "요약"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    @DisplayName("존재하지 않는 고객으로 등록하면 404와 실패 응답을 반환한다")
    void createBriefing_withNonExistingCustomer_returns404() throws Exception {
        mockMvc.perform(post("/api/briefings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 999999, "summaryText": "요약"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }

    @Test
    @DisplayName("고객별 브리핑 목록을 조회하면 최신순으로 반환한다")
    void getBriefingsByCustomer_success() throws Exception {
        createBriefing("첫 번째 브리핑");
        createBriefing("두 번째 브리핑");

        mockMvc.perform(get("/api/customers/{customerId}/briefings", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].summaryText").value("두 번째 브리핑"))
                .andExpect(jsonPath("$.result[1].summaryText").value("첫 번째 브리핑"));
    }

    @Test
    @DisplayName("브리핑 단건 조회에 성공하면 200과 브리핑 정보를 반환한다")
    void getBriefing_success() throws Exception {
        Long briefingId = createBriefing("요약 텍스트");

        mockMvc.perform(get("/api/briefings/{briefingId}", briefingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.briefingId").value(briefingId))
                .andExpect(jsonPath("$.result.summaryText").value("요약 텍스트"));
    }

    @Test
    @DisplayName("존재하지 않는 브리핑을 조회하면 404와 실패 응답을 반환한다")
    void getBriefing_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/briefings/{briefingId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }
}
