package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.dto.ApplicationRequestDto;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.beneficiary.repository.BeneficiaryRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              BeneficiaryRepository beneficiaryRepository,
                              SchemeRepository schemeRepository) {
        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
    }

    @Transactional
    public ApplicationResponseDto createApplication(ApplicationRequestDto request) {
        Beneficiary beneficiary = beneficiaryRepository.findById(request.getBeneficiaryId())
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", request.getBeneficiaryId()));

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", request.getSchemeId()));

        if (!scheme.isActive()) {
            throw new InvalidOperationException("Cannot apply to an inactive scheme.");
        }

        Application application = new Application();
        application.setBeneficiary(beneficiary);
        application.setScheme(scheme);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setEligibilityScore(0);
        application.setSubmissionDate(LocalDate.now());

        Application saved = applicationRepository.save(application);
        return toDto(saved);
    }

    public ApplicationResponseDto getApplicationById(Long id) {
        return toDto(findOrThrow(id));
    }

    public List<ApplicationResponseDto> getAllApplications() {
        return applicationRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<ApplicationResponseDto> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status).stream().map(this::toDto).toList();
    }

    private Application findOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }

    private ApplicationResponseDto toDto(Application a) {
        ApplicationResponseDto dto = new ApplicationResponseDto();
        dto.setId(a.getId());
        dto.setBeneficiaryId(a.getBeneficiary().getId());
        dto.setBeneficiaryName(a.getBeneficiary().getFullName());
        dto.setSchemeId(a.getScheme().getId());
        dto.setSchemeName(a.getScheme().getName());
        dto.setStatus(a.getStatus());
        dto.setEligibilityScore(a.getEligibilityScore());
        dto.setSubmissionDate(a.getSubmissionDate());
        dto.setRemarks(a.getRemarks());
        return dto;
    }
}