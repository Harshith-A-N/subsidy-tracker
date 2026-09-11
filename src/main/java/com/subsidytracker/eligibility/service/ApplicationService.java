package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.eligibility.dto.ApplicationRequestDto;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.beneficiary.repository.BeneficiaryRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final EligibilityService eligibilityService;
    private final AuditLogService auditLogService;
    private final SchemeSlabRepository schemeSlabRepository;
    private final VerificationRepository verificationRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              BeneficiaryRepository beneficiaryRepository,
                              SchemeRepository schemeRepository,
                              UserRepository userRepository,
                              EligibilityService eligibilityService,
                              AuditLogService auditLogService,
                              SchemeSlabRepository schemeSlabRepository,
                              VerificationRepository verificationRepository) {
        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.userRepository = userRepository;
        this.eligibilityService = eligibilityService;
        this.auditLogService = auditLogService;
        this.schemeSlabRepository = schemeSlabRepository;
        this.verificationRepository = verificationRepository;
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

        // Prevent duplicate applications: verify if an active application for this scheme already exists
        List<ApplicationStatus> terminalStatuses = List.of(
                ApplicationStatus.NOT_ELIGIBLE,
                ApplicationStatus.FIELD_REJECTED,
                ApplicationStatus.DISTRICT_REJECTED,
                ApplicationStatus.FINANCE_REJECTED,
                ApplicationStatus.APPLICATION_CANCELLED
        );
        if (applicationRepository.existsByBeneficiaryIdAndSchemeIdAndStatusNotIn(beneficiary.getId(), scheme.getId(), terminalStatuses)) {
            throw new InvalidOperationException("You already have an active application for this scheme.");
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

        // Check if scheme is active
        if (!application.getScheme().isActive()) {
            throw new InvalidOperationException("Cannot submit application: the scheme is currently inactive.");
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
        ApplicationResponseDto response = eligibilityService.calculateEligibilityForApplication(application);

        try {
            User submittingUser = userRepository.findById(currentUserId).orElse(null);
            auditLogService.logEvent(
                    "Application",
                    application.getId(),
                    "SUBMITTED",
                    submittingUser,
                    "Application submitted for eligibility evaluation. New status: " + response.getStatus());
        } catch (Exception e) {
            logger.warn("Failed to log audit event [entityType=Application, entityId={}, action=SUBMITTED]: {}",
                    application.getId(), e.getMessage(), e);
        }

        return response;
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

    public Page<ApplicationResponseDto> getAllApplications(Pageable pageable) {
        return applicationRepository.findAll(pageable).map(this::toDto);
    }

    public List<ApplicationResponseDto> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status).stream().map(this::toDto).toList();
    }

    public List<ApplicationResponseDto> getApplicationsByStatusIn(List<ApplicationStatus> statuses) {
        return applicationRepository.findByStatusIn(statuses).stream().map(this::toDto).toList();
    }

    public List<ApplicationResponseDto> getApplicationsByStatusInAndRegion(List<ApplicationStatus> statuses, String region) {
        if (region == null || region.equalsIgnoreCase("ALL") || region.equalsIgnoreCase("All Regions") || region.equalsIgnoreCase("HQ")) {
            return getApplicationsByStatusIn(statuses);
        }
        return applicationRepository.findByStatusInAndRegion(statuses, region).stream().map(this::toDto).toList();
    }

    public Page<ApplicationResponseDto> getApplicationsByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable).map(this::toDto);
    }

    public Page<ApplicationResponseDto> getApplicationsByStatusAndRegion(ApplicationStatus status, String region, Pageable pageable) {
        if (region == null || region.equalsIgnoreCase("ALL") || region.equalsIgnoreCase("All Regions") || region.equalsIgnoreCase("HQ")) {
            return getApplicationsByStatus(status, pageable);
        }
        return applicationRepository.findByStatusAndRegion(status, region, pageable).map(this::toDto);
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

    public Page<ApplicationResponseDto> getMyApplications(long currentUserId, Pageable pageable) {
        Beneficiary beneficiary = beneficiaryRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new InvalidOperationException(
                        "No beneficiary profile found for current user."));

        return applicationRepository.findByBeneficiaryId(beneficiary.getId(), pageable).map(this::toDto);
    }

    private Application findOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }

    public ApplicationResponseDto toDto(Application a) {
        ApplicationResponseDto dto = new ApplicationResponseDto();
        dto.setId(a.getId());
        dto.setBeneficiaryId(a.getBeneficiary() != null ? a.getBeneficiary().getId() : null);
        dto.setBeneficiaryName(a.getBeneficiary() != null ? a.getBeneficiary().getFullName() : null);
        dto.setSchemeId(a.getScheme() != null ? a.getScheme().getId() : null);
        dto.setSchemeName(a.getScheme() != null ? a.getScheme().getName() : null);
        dto.setStatus(a.getStatus());
        dto.setEligibilityScore(a.getEligibilityScore());
        dto.setSubmissionDate(a.getSubmissionDate());
        dto.setRemarks(a.getRemarks());

        if (a.getScheme() != null) {
            BigDecimal grant = new BigDecimal("50000.00");
            if (a.getBeneficiary() != null && a.getBeneficiary().getCategory() != null) {
                var slab = schemeSlabRepository.findBySchemeIdAndCategory(a.getScheme().getId(), a.getBeneficiary().getCategory());
                if (slab.isPresent() && slab.get().getGrantAmount() != null) {
                    grant = slab.get().getGrantAmount();
                }
            }
            dto.setRequestedAmount(grant);
            
            ApplicationResponseDto.SchemeSummaryDto schemeDto = new ApplicationResponseDto.SchemeSummaryDto();
            schemeDto.setId(a.getScheme().getId());
            schemeDto.setSchemeName(a.getScheme().getName());
            schemeDto.setName(a.getScheme().getName());
            schemeDto.setDescription(a.getScheme().getDescription());
            schemeDto.setRequiredDocuments(a.getScheme().getRequiredDocuments());
            dto.setScheme(schemeDto);
        }

        if (a.getId() != null) {
            verificationRepository.findTopByApplicationIdOrderByVerificationDateDesc(a.getId())
                    .ifPresent(v -> dto.setVerificationDate(v.getVerificationDate()));
        }

        if (a.getBeneficiary() != null) {
            ApplicationResponseDto.BeneficiarySummaryDto benDto = new ApplicationResponseDto.BeneficiarySummaryDto();
            benDto.setId(a.getBeneficiary().getId());
            benDto.setFullName(a.getBeneficiary().getFullName());
            benDto.setNationalIdNumber(a.getBeneficiary().getNationalIdNumber());
            benDto.setPhoneNumber(a.getBeneficiary().getPhoneNumber());
            benDto.setRegion(a.getBeneficiary().getRegion());
            benDto.setCategory(a.getBeneficiary().getCategory() != null ? a.getBeneficiary().getCategory().name() : null);
            dto.setBeneficiary(benDto);
        }

        return dto;
    }
}