package com.subsidytracker.eligibility.service;


import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.SchemeSlab;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.entity.Verification;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.enums.VerificationDecision;
import com.subsidytracker.common.enums.VerificationLevel;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.service.ScheduleGenerationService;
import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class VerificationService {

    // ---- Routing policy thresholds ----
    // Applications with a high eligibility score AND a low grant amount
    // are considered low-risk and can skip District review.
    private static final double FAST_TRACK_SCORE_THRESHOLD = 80.0;
    private static final BigDecimal FAST_TRACK_GRANT_LIMIT =
            new BigDecimal("50000");

    private final ApplicationRepository applicationRepository;
    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final SchemeSlabRepository schemeSlabRepository;
    private final ScheduleGenerationService scheduleGenerationService;

    public VerificationService(
            ApplicationRepository applicationRepository,
            VerificationRepository verificationRepository,
            UserRepository userRepository,
            SchemeSlabRepository schemeSlabRepository,
            ScheduleGenerationService scheduleGenerationService) {
        this.applicationRepository = applicationRepository;
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.schemeSlabRepository = schemeSlabRepository;
        this.scheduleGenerationService = scheduleGenerationService;
    }

    @Transactional
    public VerificationResponseDto processVerification(
            Long applicationId,
            VerificationRequestDto request,
            long officerId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Application",
                                applicationId));

        User officer = userRepository.findById(officerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Officer",
                                officerId));

        VerificationLevel level =
                determineLevelFromRole(officer.getRole());

        // Confirm the application is actually waiting at this stage
        validateApplicationIsAtStage(application, level);

        // Confirm the officer is responsible for this beneficiary's region
        validateOfficerRegion(officer, application);

        // Record the decision
        Verification verification = new Verification();

        verification.setApplication(application);
        verification.setOfficer(officer);
        verification.setLevel(level);
        verification.setDecision(request.getDecision());
        verification.setRemarks(request.getRemarks());

        verificationRepository.save(verification);

        // Advance or send back the application
        ApplicationStatus newStatus =
                routeApplication(
                        request.getDecision(),
                        level,
                        application);

        application.setStatus(newStatus);
        application.setRemarks(request.getRemarks());

        applicationRepository.save(application);

        if (newStatus == ApplicationStatus.READY_FOR_DISBURSEMENT) {
            scheduleGenerationService.generateSchedule(application.getId());
        }

        VerificationResponseDto response =
                new VerificationResponseDto();
        response.setVerificationId(verification.getId());
        response.setApplicationId(application.getId());
        response.setNewStatus(newStatus);
        response.setMessage(
                "Verification recorded at "
                        + level
                        + " level.");

        return response;
    }

    @Transactional
    public VerificationResponseDto resumeAfterReVerification(
            Long applicationId) {

        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Application",
                                        applicationId));

        if (application.getStatus()
                != ApplicationStatus.RE_VERIFICATION_REQUIRED) {

            throw new InvalidOperationException(
                    "Application is not awaiting re-verification. "
                            + "Current status: "
                            + application.getStatus());
        }

        Verification lastAction =
                verificationRepository
                        .findTopByApplicationIdOrderByVerificationDateDesc(
                                applicationId)
                        .orElseThrow(() ->
                                new InvalidOperationException(
                                        "No prior verification history found "
                                                + "- cannot determine which "
                                                + "stage to resume at."));

        ApplicationStatus resumeStatus;

        if (lastAction.getLevel()
                == VerificationLevel.FIELD) {

            resumeStatus =
                    ApplicationStatus.FIELD_VERIFICATION_PENDING;

        } else if (lastAction.getLevel()
                == VerificationLevel.DISTRICT) {

            resumeStatus =
                    ApplicationStatus.DISTRICT_REVIEW_PENDING;

        } else if (lastAction.getLevel()
                == VerificationLevel.FINANCE) {

            resumeStatus =
                    ApplicationStatus.FINANCE_REVIEW_PENDING;

        } else {

            throw new InvalidOperationException(
                    "Unsupported verification level: "
                            + lastAction.getLevel());
        }

        application.setStatus(resumeStatus);
        applicationRepository.save(application);

        VerificationResponseDto response =
                new VerificationResponseDto();

        response.setApplicationId(application.getId());
        response.setNewStatus(resumeStatus);
        response.setMessage(
                "Application resumed at "
                        + lastAction.getLevel()
                        + " level after re-verification.");

        return response;
    }

    // ---- Role -> Level mapping ----
    private VerificationLevel determineLevelFromRole(Role role) {

        if (role == Role.FIELD_OFFICER) {
            return VerificationLevel.FIELD;
        }

        if (role == Role.DISTRICT_OFFICER) {
            return VerificationLevel.DISTRICT;
        }

        if (role == Role.FINANCE_APPROVER) {
            return VerificationLevel.FINANCE;
        }

        throw new InvalidOperationException(
                "This role is not authorized to perform verification.");
    }

    // ---- Confirm application is genuinely waiting at this stage ----
    private void validateApplicationIsAtStage(
            Application application,
            VerificationLevel level) {

        ApplicationStatus status =
                application.getStatus();

        boolean valid;

        if (level == VerificationLevel.FIELD) {

            valid =
                    status
                            == ApplicationStatus.FIELD_VERIFICATION_PENDING;

        } else if (level == VerificationLevel.DISTRICT) {

            valid =
                    status
                            == ApplicationStatus.DISTRICT_REVIEW_PENDING;

        } else if (level == VerificationLevel.FINANCE) {

            valid =
                    status
                            == ApplicationStatus.FINANCE_REVIEW_PENDING;

        } else {

            valid = false;
        }

        if (!valid) {

            throw new InvalidOperationException(
                    "Application is not currently pending "
                            + level
                            + " review. Current status: "
                            + status);
        }
    }

    // ---- Region-based routing check ----
    // FINANCE_APPROVER and ADMIN operate across all regions.
    // FIELD_OFFICER and DISTRICT_OFFICER must match the
    // beneficiary's region.
    private void validateOfficerRegion(
            User officer,
            Application application) {

        Role role = officer.getRole();

        if (role == Role.FINANCE_APPROVER
                || role == Role.ADMIN) {

            return;
        }

        String beneficiaryRegion =
                application.getBeneficiary().getRegion();

        String officerRegion =
                officer.getRegion();

        if (beneficiaryRegion == null
                || officerRegion == null
                || !beneficiaryRegion.equalsIgnoreCase(
                officerRegion)) {

            throw new InvalidOperationException(
                    "Officer's region ("
                            + officerRegion
                            + ") does not match beneficiary's region ("
                            + beneficiaryRegion
                            + "). This officer cannot act on this application.");
        }
    }

    // ---- Decision -> next ApplicationStatus ----
    private ApplicationStatus routeApplication(
            VerificationDecision decision,
            VerificationLevel level,
            Application application) {

        // Re-verification can be requested at any stage
        if (decision
                == VerificationDecision.RE_VERIFICATION_REQUESTED) {

            return ApplicationStatus.RE_VERIFICATION_REQUIRED;
        }

        // Rejected
        if (decision == VerificationDecision.REJECTED) {

            if (level == VerificationLevel.FIELD) {

                return ApplicationStatus.FIELD_REJECTED;
            }

            if (level == VerificationLevel.DISTRICT) {

                return ApplicationStatus.DISTRICT_REJECTED;
            }

            if (level == VerificationLevel.FINANCE) {

                return ApplicationStatus.FINANCE_REJECTED;
            }

            throw new InvalidOperationException(
                    "Unsupported verification level: "
                            + level);
        }

        // Approved - advance to next stage

        if (level == VerificationLevel.FIELD) {

            // Low-risk applications skip District review
            if (shouldFastTrack(application)) {

                return ApplicationStatus.FINANCE_REVIEW_PENDING;
            }

            return ApplicationStatus.DISTRICT_REVIEW_PENDING;
        }

        if (level == VerificationLevel.DISTRICT) {

            return ApplicationStatus.FINANCE_REVIEW_PENDING;
        }

        if (level == VerificationLevel.FINANCE) {

            return ApplicationStatus.READY_FOR_DISBURSEMENT;
        }

        throw new InvalidOperationException(
                "Unsupported verification level: "
                        + level);
    }

    /**
     * Determines whether an application qualifies for fast-track routing
     * by skipping District review after Field approval.
     *
     * Conditions:
     * 1. Eligibility score >= FAST_TRACK_SCORE_THRESHOLD
     * 2. Grant amount <= FAST_TRACK_GRANT_LIMIT
     *
     * If no matching SchemeSlab exists, the application
     * is NOT fast-tracked.
     */
    private boolean shouldFastTrack(
            Application application) {

        // Condition 1: high eligibility score
        if (application.getEligibilityScore()
                < FAST_TRACK_SCORE_THRESHOLD) {

            return false;
        }

        // Condition 2: low grant amount
        Beneficiary beneficiary =
                application.getBeneficiary();

        Long schemeId =
                application.getScheme().getId();

        Optional<SchemeSlab> slab =
                schemeSlabRepository.findBySchemeIdAndCategory(
                        schemeId,
                        beneficiary.getCategory());

        if (slab.isEmpty()) {

            return false;
        }

        return slab.get()
                .getGrantAmount()
                .compareTo(FAST_TRACK_GRANT_LIMIT) <= 0;
    }
}
