package com.example.suphernova.domain.briefing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.suphernova.domain.briefing.client.AiBriefingGenerationClient;
import com.example.suphernova.domain.briefing.client.AiBriefingGenerationResult;
import com.example.suphernova.domain.briefing.client.SttClient;
import com.example.suphernova.domain.briefing.client.TtsClient;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.domain.store.entity.Store;
import com.example.suphernova.domain.store.repository.StoreRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BriefingAudioIngestionControllerTest {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @MockitoBean
    private SttClient sttClient;

    @MockitoBean
    private TtsClient ttsClient;

    @MockitoBean
    private AiBriefingGenerationClient aiBriefingGenerationClient;

    @org.springframework.beans.factory.annotation.Value("${app.internal.token}")
    private String internalToken;

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
    @DisplayName("오디오를 업로드하면 브리핑이 PENDING/PROCESSING 상태로 생성되고 STT 전사를 요청한다")
    void ingestAudio_success_startsProcessing() throws Exception {
        when(sttClient.requestTranscription(any(), anyLong())).thenReturn("stt-job-1");

        MockMultipartFile audio = new MockMultipartFile("audio", "recording.wav", "audio/wav", "fake-audio".getBytes());

        mockMvc.perform(multipart("/api/customers/{customerId}/briefings/audio", customerId).file(audio))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON202_1"))
                .andExpect(jsonPath("$.result.briefingId").isNumber())
                .andExpect(jsonPath("$.result.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("STT 요청 자체가 실패하면 브리핑 상태가 API_FAIL이 된다")
    void ingestAudio_sttRequestThrows_marksApiFail() throws Exception {
        when(sttClient.requestTranscription(any(), anyLong())).thenThrow(new RestClientException("connection refused"));

        MockMultipartFile audio = new MockMultipartFile("audio", "recording.wav", "audio/wav", "fake-audio".getBytes());

        mockMvc.perform(multipart("/api/customers/{customerId}/briefings/audio", customerId).file(audio))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result.status").value("API_FAIL"));
    }

    @Test
    @DisplayName("STT 콜백이 오면 스크립트를 생성하고 TTS를 요청, 이후 TTS 콜백을 받으면 SUCCESS가 된다")
    void fullPipeline_sttThenTtsCallback_endsInSuccess() throws Exception {
        when(sttClient.requestTranscription(any(), anyLong())).thenReturn("stt-job-1");
        when(aiBriefingGenerationClient.generate(any()))
                .thenReturn(new AiBriefingGenerationResult("요약", "지난번 문의하신 블랙 로퍼가 오늘 입고됐습니다.", List.of("블랙")));
        when(ttsClient.requestSynthesis(any(), anyLong())).thenReturn("tts-job-1");

        MockMultipartFile audio = new MockMultipartFile("audio", "recording.wav", "audio/wav", "fake-audio".getBytes());
        String ingestBody = mockMvc.perform(multipart("/api/customers/{customerId}/briefings/audio", customerId).file(audio))
                .andReturn().getResponse().getContentAsString();
        Long briefingId = JsonPath.parse(ingestBody).read("$.result.briefingId", Long.class);

        String sttCallbackJson = """
                {"jobId": "stt-job-1", "briefingId": %d, "status": "SUCCESS", "text": "고객이 블랙 로퍼를 문의함"}
                """.formatted(briefingId);
        mockMvc.perform(post("/internal/stt-callback")
                        .header(INTERNAL_TOKEN_HEADER, internalToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(sttCallbackJson))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/briefings/{briefingId}", briefingId))
                .andExpect(jsonPath("$.result.status").value("PROCESSING"))
                .andExpect(jsonPath("$.result.scriptText").value("지난번 문의하신 블랙 로퍼가 오늘 입고됐습니다."));

        MockMultipartFile ttsAudio = new MockMultipartFile("audio", "tts.mp3", "audio/mpeg", "fake-tts-audio".getBytes());
        mockMvc.perform(multipart("/internal/tts-callback")
                        .file(ttsAudio)
                        .param("briefingId", String.valueOf(briefingId))
                        .param("status", "SUCCESS")
                        .param("durationSeconds", "12")
                        .header(INTERNAL_TOKEN_HEADER, internalToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/briefings/{briefingId}", briefingId))
                .andExpect(jsonPath("$.result.status").value("SUCCESS"))
                .andExpect(jsonPath("$.result.ttsDuration").value(12))
                .andExpect(jsonPath("$.result.ttsAudioUrl").isNotEmpty());
    }

    @Test
    @DisplayName("잘못된 내부 토큰으로 콜백을 호출하면 401을 반환한다")
    void sttCallback_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(post("/internal/stt-callback")
                        .header(INTERNAL_TOKEN_HEADER, "wrong-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"briefingId": 1, "status": "SUCCESS", "text": "텍스트"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON401_1"));
    }
}
