package com.example.suphernova.domain.store.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("매장 등록에 성공하면 201과 생성된 매장 정보를 반환한다")
    void createStore_success() throws Exception {
        mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName": "성신여대점", "managerName": "김수진"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201_1"))
                .andExpect(jsonPath("$.result.storeId").isNumber())
                .andExpect(jsonPath("$.result.storeName").value("성신여대점"))
                .andExpect(jsonPath("$.result.managerName").value("김수진"));
    }

    @Test
    @DisplayName("매장명 없이 등록하면 400과 실패 응답을 반환한다")
    void createStore_withoutStoreName_returns400() throws Exception {
        mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managerName": "김수진"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }
}
