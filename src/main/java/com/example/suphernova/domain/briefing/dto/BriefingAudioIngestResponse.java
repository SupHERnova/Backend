package com.example.suphernova.domain.briefing.dto;

import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;

public record BriefingAudioIngestResponse(
        Long briefingId,
        BriefingStatus status
) {

    public static BriefingAudioIngestResponse from(Briefing briefing) {
        return new BriefingAudioIngestResponse(briefing.getId(), briefing.getStatus());
    }
}
