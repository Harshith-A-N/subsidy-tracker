package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.dto.ApplicationRequestDto;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final EligibilityService eligibilityService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              BeneficiaryRepository beneficiaryRepository,
                              SchemeRepository schemeRepository,
                              UserRepository userRepository,
                              EligibilityService eligibilityService) {
        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.userRepository = userRepository;
        this.eligibilityService = eligibilityService;
    }

    /**
     * Creates a draft application for the authenticated beneficiary.
     * The beneficiary is resolved from the authenticated user's ID —
     * never from client-supplied data.
     *
     * The application starts in DRAFT status. Documents must be uploaded
     * before calling submitApplication() to trigger eligibility.
     */
    @Transactional
    public ApplicationResponseDto createApplication(ApplicationRequestDto request, long currentUserId) {
        // Verify the user exists and has the BENEFICIARY role
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (user.getRole() != Role.BENEFICIARY) {
            throw new InvalidOperationException("Only users with BENEFICIARY role can create applications.");
        }

        // Resolve beneficiary profile from the authenticated user
        Beneficiary beneficiary = beneficiaryRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new InvalidOperationException(
                        "You must create a beneficiary profile before creating an application."));

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", request.getSchemeId()));

        if (!scheme.isActive()) {
            throw new InvalidOperationException("Cannot apply to an inactive scheme.");
        }

        Application application = new Application();
        application.setBeneficiary(beneficiary);
        application.setScheme(scheme);
        application.setStatus(ApplicationStatus.DRAFT);
        application.setEligibilityScore(0);
        application.setSubmissionDate(LocalDate.now());

        Application saved = applicationRepository.save(application);
        return toDto(saved);
    }

    /**
     * Formally submits a draft application for eligibility evaluation.
     * This is the trigger point for EligibilityService — called only after
     * the beneficiary has uploaded required documents.
     */
    @Transactional
    public ApplicationResponseDto submitApplication(Long applicationId, long currentUserId) {
        Application application = findOrThrow(applicationId);

        // Only DRAFT applications can be submitted
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidOperationException(
                    "Only DRAFT applications can be submitted. Current status: " + application.getStatus());
        }

        // Ownership check: the authenticated user must own this application
        Beneficiary beneficiary = application.getBeneficiary();
        if (beneficiary.getUser() == null || beneficiary.getUser().getId() != currentUserId) {
            throw new InvalidOperationException("You are not authorized to submit this application.");
        }

        // Validate mandatory documents before invoking EligibilityService
        List<String> missingDocs = eligibilityService.getMissingMandatoryDocuments(application);
        if (!missingDocs.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Missing required documents:");
            for (String doc : missingDocs) {
                errorMsg.append("\n- ").append(doc);
            }
            throw new InvalidOperationException(errorMsg.toString());
        }

        // Trigger eligibility calculation — evaluates income, category, region
        return eligibilityService.calculateEligibilityForApplication(application);
    }

    /**
     * Returns a single application by ID.
     * Beneficiaries can only access their own applications.
     * Officers and admins can access any application.
     */
    public ApplicationResponseDto getApplicationById(Long id, long currentUserId) {
        Application application = findOrThrow(id);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (currentUser.getRole() == Role.BENEFICIARY) {
            Beneficiary beneficiary = application.getBeneficiary();
            if (beneficiary.getUser() == null || beneficiary.getUser().getId() != currentUserId) {
                throw new InvalidOperationException("You are not authorized to view this application.");
            }
        }

        return toDto(application);
    }

    public List<ApplicationResponseDto> getAllApplications() {
        return applicationRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<ApplicationResponseDto> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status).stream().map(this::toDto).toList();
    }

    /**
     * Returns only applications belonging to the authenticated beneficiary.
     */
    public List<ApplicationResponseDto> getMyApplications(long currentUserId) {
        Beneficiary beneficiary = beneficiaryRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new InvalidOperationException(
                        "No beneficiary profile found for current user."));

        return applicationRepository.findByBeneficiaryId(beneficiary.getId())
                .stream().map(this::toDto).toList();
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