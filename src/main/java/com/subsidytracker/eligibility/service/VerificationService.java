package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.entity.Verification;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.enums.VerificationDecision;
import com.subsidytracker.common.enums.VerificationLevel;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {

    private final ApplicationRepository applicationRepository;
    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;

    public VerificationService(ApplicationRepository applicationRepository,
                               VerificationRepository verificationRepository,
                               UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VerificationResponseDto processVerification(Long applicationId, VerificationRequestDto request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        User officer = userRepository.findById(request.getOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException("Officer", request.getOfficerId()));

        VerificationLevel level = determineLevelFromRole(officer.getRole());

        // Confirm the application is actually waiting at this stage
        validateApplicationIsAtStage(application, level);

        // Confirm the officer is actually responsible for this beneficiary's region
        validateOfficerRegion(officer, application);

        // Record the decision - a new row every time, never overwritten
        Verification verification = new Verification();
        verification.setApplication(application);
        verification.setOfficer(officer);
        verification.setLevel(level);
        verification.setDecision(request.getDecision());
        verification.setRemarks(request.getRemarks());
        verificationRepository.save(verification);

        // Advance (or send back) the application based on the decision
        ApplicationStatus newStatus = routeApplication(request.getDecision(), level);
        application.setStatus(newStatus);
        application.setRemarks(request.getRemarks());
        applicationRepository.save(application);

        VerificationResponseDto response = new VerificationResponseDto();
        response.setVerificationId(verification.getId());
        response.setApplicationId(application.getId());
        response.setNewStatus(newStatus);
        response.setMessage("Verification recorded at " + level + " level.");
        return response;
    }

    @Transactional
    public VerificationResponseDto resumeAfterReVerification(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (application.getStatus() != ApplicationStatus.RE_VERIFICATION_REQUIRED) {
            throw new InvalidOperationException(
                    "Application is not awaiting re-verification. Current status: " + application.getStatus());
        }

        Verification lastAction = verificationRepository
                .findTopByApplicationIdOrderByVerificationDateDesc(applicationId)
                .orElseThrow(() -> new InvalidOperationException(
                        "No prior verification history found - cannot determine which stage to resume at."));

        ApplicationStatus resumeStatus = switch (lastAction.getLevel()) {
            case FIELD -> ApplicationStatus.FIELD_VERIFICATION_PENDING;
            case DISTRICT -> ApplicationStatus.DISTRICT_REVIEW_PENDING;
            case FINANCE -> ApplicationStatus.FINANCE_REVIEW_PENDING;
        };

        application.setStatus(resumeStatus);
        applicationRepository.save(application);

        VerificationResponseDto response = new VerificationResponseDto();
        response.setApplicationId(application.getId());
        response.setNewStatus(resumeStatus);
        response.setMessage("Application resumed at " + lastAction.getLevel() + " level after re-verification.");
        return response;
    }

    // ---- Role -> Level mapping ----
    private VerificationLevel determineLevelFromRole(Role role) {
        return switch (role) {
            case FIELD_OFFICER -> VerificationLevel.FIELD;
            case DISTRICT_OFFICER -> VerificationLevel.DISTRICT;
            case FINANCE_APPROVER -> VerificationLevel.FINANCE;
            default -> throw new InvalidOperationException("This role is not authorized to perform verification.");
        };
    }

    // ---- Confirm application is genuinely waiting at this stage ----
    private void validateApplicationIsAtStage(Application application, VerificationLevel level) {
        ApplicationStatus status = application.getStatus();

        boolean valid = switch (level) {
            case FIELD -> status == ApplicationStatus.FIELD_VERIFICATION_PENDING;
            case DISTRICT -> status == ApplicationStatus.DISTRICT_REVIEW_PENDING;
            case FINANCE -> status == ApplicationStatus.FINANCE_REVIEW_PENDING;
        };

        if (!valid) {
            throw new InvalidOperationException(
                    "Application is not currently pending " + level + " review. Current status: " + status);
        }
    }

    // ---- Region-based routing check (your routing-logic.md update) ----
    private void validateOfficerRegion(User officer, Application application) {
        String beneficiaryRegion = application.getBeneficiary().getRegion();
        String officerRegion = officer.getRegion();

        if (beneficiaryRegion == null || officerRegion == null
                || !beneficiaryRegion.equalsIgnoreCase(officerRegion)) {
            throw new InvalidOperationException(
                    "Officer's region (" + officerRegion + ") does not match beneficiary's region ("
                            + beneficiaryRegion + "). This officer cannot act on this application.");
        }
    }

    // ---- Decision -> next ApplicationStatus, using YOUR finalized enum ----
    private ApplicationStatus routeApplication(VerificationDecision decision, VerificationLevel level) {

        // Re-verification can be requested at any stage - sent back for correction
        if (decision == VerificationDecision.RE_VERIFICATION_REQUESTED) {
            return ApplicationStatus.RE_VERIFICATION_REQUIRED;
        }

        if (decision == VerificationDecision.REJECTED) {
            return switch (level) {
                case FIELD -> ApplicationStatus.FIELD_REJECTED;
                case DISTRICT -> ApplicationStatus.DISTRICT_REJECTED;
                case FINANCE -> ApplicationStatus.FINANCE_REJECTED;
            };
        }

        // APPROVED - advance to the next stage
        return switch (level) {
            case FIELD -> {
                yield ApplicationStatus.DISTRICT_REVIEW_PENDING;
            }
            case DISTRICT -> ApplicationStatus.FINANCE_REVIEW_PENDING;
            case FINANCE -> ApplicationStatus.READY_FOR_DISBURSEMENT;
        };
    }
}