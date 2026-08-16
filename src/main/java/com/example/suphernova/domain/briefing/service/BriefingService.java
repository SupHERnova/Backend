package com.example.suphernova.domain.briefing.service;

import com.example.suphernova.domain.briefing.dto.BriefingCreateRequest;
import com.example.suphernova.domain.briefing.dto.BriefingResponse;
import com.example.suphernova.domain.briefing.entity.Briefing;
import com.example.suphernova.domain.briefing.entity.BriefingStatus;
import com.example.suphernova.domain.briefing.repository.BriefingRepository;
import com.example.suphernova.domain.customer.entity.Customer;
import com.example.suphernova.domain.customer.repository.CustomerRepository;
import com.example.suphernova.global.apiPayload.code.GeneralErrorCode;
import com.example.suphernova.global.apiPayload.exception.ProjectException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BriefingService {

    private final BriefingRepository briefingRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public BriefingResponse createBriefing(BriefingCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        Briefing briefing = Briefing.builder()
                .customer(customer)
                .summaryText(request.summaryText())
                .scriptText(request.scriptText())
                .ttsAudioUrl(request.ttsAudioUrl())
                .ttsDuration(request.ttsDuration())
                .status(request.status() != null ? request.status() : BriefingStatus.SUCCESS)
                .build();

        return BriefingResponse.from(briefingRepository.save(briefing));
    }

    @Transactional(readOnly = true)
    public List<BriefingResponse> getBriefingsByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ProjectException(GeneralErrorCode.NOT_FOUND);
        }

        return briefingRepository.findAllByCustomerIdOrderByCreatedAtDescIdDesc(customerId).stream()
                .map(BriefingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BriefingResponse getBriefing(Long briefingId) {
        return briefingRepository.findById(briefingId)
                .map(BriefingResponse::from)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
    }
}
