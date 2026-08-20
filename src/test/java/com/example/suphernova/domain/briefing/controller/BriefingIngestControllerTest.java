package com.example.suphernova.domain.briefing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suphernova.domain.briefing.client.TtsClient;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BriefingIngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @MockitoBean
    private TtsClient ttsClient;

    private Long customerId;

    @BeforeEach
    void setUp() {
        Store store = storeRepository.save(Store.builder()
                .storeName("성신여대점")
                .managerName("김수진")
                .build());
        Customer customer = customerRepository.save(Customer.builder()
                .store(store)
                .customerName("김서윤")
                .build());
        customerId = customer.getId();
    }

    @Test
    @DisplayName("전사 텍스트를 보내면 추천 상태(선호 키워드/재입고 여부)에 반영된 내용을 바탕으로 브리핑 생성과 TTS 합성이 동기로 완료되어 SUCCESS를 반환한다")
    void ingestText_success_returnsSuccess() throws Exception {
        when(ttsClient.synthesize(any())).thenReturn("fake-tts-audio".getBytes());

        mockMvc.perform(post("/api/customers/{customerId}/briefings/generate", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transcribedText": "지난번 문의하신 블랙 로퍼가 오늘 입고됐습니다."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201_1"))
                .andExpect(jsonPath("$.result.briefingId").isNumber())
                .andExpect(jsonPath("$.result.status").value("SUCCESS"))
                .andExpect(jsonPath("$.result.scriptText").value(
                        "김서윤 고객님 방문 전 브리핑입니다. 해결되지 않은 재입고 요청은 없습니다. "))
                .andExpect(jsonPath("$.result.ttsAudioUrl").isNotEmpty());
    }

    @Test
    @DisplayName("TTS 합성이 실패하면 API_FAIL 상태를 반환한다")
    void ingestText_ttsFails_returnsApiFail() throws Exception {
        when(ttsClient.synthesize(any())).thenThrow(new RestClientException("connection refused"));

        mockMvc.perform(post("/api/customers/{customerId}/briefings/generate", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transcribedText": "지난번 문의하신 블랙 로퍼가 오늘 입고됐습니다."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.status").value("API_FAIL"));
    }

    @Test
    @DisplayName("전사 텍스트 없이 요청하면 400을 반환한다")
    void ingestText_withoutText_returns400() throws Exception {
        mockMvc.perform(post("/api/customers/{customerId}/briefings/generate", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transcribedText": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    @DisplayName("존재하지 않는 고객이면 404를 반환한다")
    void ingestText_withNonExistingCustomer_returns404() throws Exception {
        mockMvc.perform(post("/api/customers/{customerId}/briefings/generate", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transcribedText": "안녕하세요"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON404_1"));
    }
}
