package com.example.suphernova.domain.briefing.entity;

import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "briefing")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Briefing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "briefing_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "script_text", columnDefinition = "TEXT")
    private String scriptText;

    @Column(name = "tts_audio_url", length = 500)
    private String ttsAudioUrl;

    @Column(name = "tts_duration")
    private Integer ttsDuration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BriefingStatus status = BriefingStatus.SUCCESS;

    @Column(name = "input_audio_url", length = 500)
    private String inputAudioUrl;

    @Column(name = "stt_job_id", length = 100)
    private String sttJobId;

    public static Briefing pending(Customer customer, String inputAudioUrl) {
        return Briefing.builder()
                .customer(customer)
                .inputAudioUrl(inputAudioUrl)
                .status(BriefingStatus.PENDING)
                .build();
    }

    public void markProcessing(String sttJobId) {
        this.sttJobId = sttJobId;
        this.status = BriefingStatus.PROCESSING;
    }

    public void applyGeneratedContent(String summaryText, String scriptText) {
        this.summaryText = summaryText;
        this.scriptText = scriptText;
    }

    public void applyTtsResult(String ttsAudioUrl, Integer ttsDuration) {
        this.ttsAudioUrl = ttsAudioUrl;
        this.ttsDuration = ttsDuration;
        this.status = BriefingStatus.SUCCESS;
    }

    public void markNoHistory() {
        this.status = BriefingStatus.NO_HISTORY;
    }

    public void markFailed() {
        this.status = BriefingStatus.API_FAIL;
    }
}
