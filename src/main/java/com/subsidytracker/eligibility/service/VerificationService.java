package com.subsidytracker.eligibility.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.entity.SchemeSlab;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.entity.Verification;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.enums.VerificationDecision;
import com.subsidytracker.common.enums.VerificationLevel;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.disbursement.service.ScheduleGenerationService;
import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import com.subsidytracker.common.enums.DocumentVerificationStatus;

@Service
public class VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

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
    private final AuditLogService auditLogService;
    private final DocumentRepository documentRepository;

    public VerificationService(
            ApplicationRepository applicationRepository,
            VerificationRepository verificationRepository,
            UserRepository userRepository,
            SchemeSlabRepository schemeSlabRepository,
            ScheduleGenerationService scheduleGenerationService,
            AuditLogService auditLogService,
            DocumentRepository documentRepository) {

        this.applicationRepository = applicationRepository;
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.schemeSlabRepository = schemeSlabRepository;
        this.scheduleGenerationService = scheduleGenerationService;
        this.auditLogService = auditLogService;
        this.documentRepository = documentRepository;
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

        // A Field Officer approval is the only point where anyone actually
        // inspects the beneficiary's KYC documents. Nothing here previously
        // checked Document.verificationStatus, so an application could be
        // approved and routed on to District/Finance review while every
        // document (Aadhar, Land Record, Income Certificate, ...) was still
        // sitting at PENDING - i.e. never actually looked at. Reject/re-
        // verification decisions are unaffected since they don't advance
        // the application past this officer.
        if (level == VerificationLevel.FIELD
                && request.getDecision() == VerificationDecision.APPROVED) {

            validateAllDocumentsVerified(application);
        }

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

        // Finance approval is the last verification step. Once an application
        // reaches READY_FOR_DISBURSEMENT, the project guide's "done looks like"
        // for Module 3 expects the disbursement schedule (and its compliance
        // milestones) to be generated automatically, not via a manually-hit
        // endpoint. Runs in the same transaction as the approval itself, so a
        // missing disbursement plan for the scheme fails the approval loudly
        // instead of leaving the application silently stuck.
        if (newStatus == ApplicationStatus.READY_FOR_DISBURSEMENT) {
            scheduleGenerationService.generateSchedule(application.getId());
        }

        try {
            auditLogService.logEvent(
                    "Application",
                    application.getId(),
                    request.getDecision().name(),
                    officer,
                    "Verification decision " + request.getDecision() + " at " + level + " level. New status: " + newStatus);
        } catch (Exception e) {
            logger.warn("Failed to log audit event [entityType=Application, entityId={}, action={}, user={}]: {}",
                    application.getId(), request.getDecision().name(), officer != null ? officer.getEmail() : "null", e.getMessage(), e);
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

    // ---- Confirm every KYC document has actually been checked ----
    // Only KYC documents (stage == null) count here - disbursement-phase
    // utilization proofs are a separate, later verification concern.
    private void validateAllDocumentsVerified(Application application) {

        List<Document> documents =
                documentRepository.findByApplicationIdAndStageIsNull(
                        application.getId());

        if (documents.isEmpty()) {

            throw new InvalidOperationException(
                    "Cannot approve: no documents have been submitted "
                            + "for this application.");
        }

        boolean hasUncheckedDocument =
                documentRepository
                        .existsByApplicationIdAndStageIsNullAndVerificationStatusNot(
                                application.getId(),
                                DocumentVerificationStatus.VERIFIED);

        if (hasUncheckedDocument) {

            throw new InvalidOperationException(
                    "Cannot approve: one or more submitted documents have "
                            + "not been marked VERIFIED yet. Every document "
                            + "must be individually reviewed before the "
                            + "application can be approved.");
        }
    }

    // ---- Decision -> next ApplicationStatus ----
    private ApplicationStatus routeApplication(
            VerificationDecision decision,
            VerificationLevel level,
            Application application) {

        // Re-verification: route back to the appropriate level immediately.
        //
        // Previously this returned RE_VERIFICATION_REQUIRED and relied on a
        // separate resumeAfterReVerification() call to route it onward -
        // but nothing in the frontend ever calls that endpoint, so the
        // application was permanently stuck: not in any officer's queue,
        // since every queue filters on its own *_PENDING status only.
        //
        // District/Finance requesting re-verification means they want the
        // level below them to redo their check (District doubts Field's
        // ground verification; Finance doubts District's review) - not for
        // themselves to review it again unchanged.
        if (decision
                == VerificationDecision.RE_VERIFICATION_REQUESTED) {

            if (level == VerificationLevel.FIELD) {
                return ApplicationStatus.FIELD_VERIFICATION_PENDING;
            }

            if (level == VerificationLevel.DISTRICT) {
                return ApplicationStatus.FIELD_VERIFICATION_PENDING;
            }

            if (level == VerificationLevel.FINANCE) {
                return ApplicationStatus.DISTRICT_REVIEW_PENDING;
            }

            throw new InvalidOperationException(
                    "Unsupported verification level: " + level);
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