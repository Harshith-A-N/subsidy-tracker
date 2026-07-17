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
import com.subsidytracker.eligibility.dto.VerificationRequestDTO;
import com.subsidytracker.eligibility.dto.VerificationResponseDTO;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public VerificationResponseDTO processVerification(Long applicationId, VerificationRequestDTO request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        User officer = userRepository.findById(request.getOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException("Officer", request.getOfficerId()));

        VerificationLevel level = determineLevelFromRole(officer.getRole());
        validateApplicationState(application, level);

        Verification verification = new Verification();
        verification.setApplication(application);
        verification.setOfficer(officer);
        verification.setOfficerRole(officer.getRole().name());
        verification.setLevel(level);
        verification.setDecision(request.getDecision());
        verification.setRemarks(request.getRemarks());
        verification.setVerificationDate(LocalDateTime.now());

        verificationRepository.save(verification);

        updateApplicationRouting(application, request.getDecision(), level);
        applicationRepository.save(application);

        return VerificationResponseDTO.builder()
                .verificationId(verification.getId())
                .applicationId(application.getId())
                .newStatus(application.getStatus().name())
                .newStage(application.getCurrentStage())
                .message("Verification recorded successfully.")
                .build();
    }

    private VerificationLevel determineLevelFromRole(Role role) {
        return switch (role) {
            case FIELD_OFFICER -> VerificationLevel.FIELD;
            case DISTRICT_OFFICER -> VerificationLevel.DISTRICT;
            case FINANCE_APPROVER -> VerificationLevel.FINANCE;
            default -> throw new InvalidOperationException("User role not authorized for verification.");
        };
    }

    private void validateApplicationState(Application application, VerificationLevel level) {
        ApplicationStatus status = application.getStatus();
        
        boolean valid = switch (level) {
            case FIELD -> status == ApplicationStatus.PENDING_FIELD_REVIEW;
            case DISTRICT -> status == ApplicationStatus.PENDING_DISTRICT_REVIEW;
            case FINANCE -> status == ApplicationStatus.PENDING_FINANCE_REVIEW;
        };

        if (!valid) {
            throw new InvalidOperationException("Application is not in the correct state for " + level + " review. Current status: " + status);
        }
    }

    private void updateApplicationRouting(Application application, VerificationDecision decision, VerificationLevel level) {
        if (decision == VerificationDecision.REJECTED) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setCurrentStage(null);
            return;
        }

        if (decision == VerificationDecision.RE_VERIFICATION) {
            application.setStatus(ApplicationStatus.RE_VERIFICATION_REQUESTED);
            application.setCurrentStage("NEEDS_APPLICANT_UPDATE");
            return;
        }

        // Handle APPROVED routing
        switch (level) {
            case FIELD -> {
                application.setStatus(ApplicationStatus.PENDING_DISTRICT_REVIEW);
                application.setCurrentStage("DISTRICT_REVIEW");
            }
            case DISTRICT -> {
                application.setStatus(ApplicationStatus.PENDING_FINANCE_REVIEW);
                application.setCurrentStage("FINANCE_REVIEW");
            }
            case FINANCE -> {
                application.setStatus(ApplicationStatus.APPROVED);
                application.setCurrentStage("READY_FOR_DISBURSEMENT");
            }
        }
    }
}
