package com.subsidytracker.eligibility.service;

import com.subsidytracker.common.entity.*;
import com.subsidytracker.common.enums.*;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.disbursement.service.ScheduleGenerationService;
import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private VerificationRepository verificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SchemeSlabRepository schemeSlabRepository;
    @Mock private ScheduleGenerationService scheduleGenerationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private VerificationService verificationService;

    private Application application;
    private User officer;
    private Beneficiary beneficiary;

    @BeforeEach
    void setUp() {
        beneficiary = new Beneficiary();
        beneficiary.setRegion("North");

        application = new Application();
        application.setId(100L);
        application.setBeneficiary(beneficiary);
        application.setStatus(ApplicationStatus.FIELD_VERIFICATION_PENDING);

        officer = new User();
        officer.setId(5L);
        officer.setRole(Role.FIELD_OFFICER);
        officer.setRegion("North");
    }

    @Test
    void processVerification_ShouldRecordVerificationAndLogAudit() {
        VerificationRequestDto request = new VerificationRequestDto();
        request.setDecision(VerificationDecision.APPROVED);
        request.setRemarks("Looks good");

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(userRepository.findById(5L)).thenReturn(Optional.of(officer));

        VerificationResponseDto response = verificationService.processVerification(100L, request, 5L);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationId()).isEqualTo(100L);
        verify(verificationRepository).save(any(Verification.class));
        verify(auditLogService).logEvent(eq("Application"), eq(100L), eq("APPROVED"), eq(officer), anyString());
    }

    @Test
    void resumeAfterReVerification_ShouldResumeAndLogAudit() {
        application.setStatus(ApplicationStatus.RE_VERIFICATION_REQUIRED);
        Verification lastVerification = new Verification();
        lastVerification.setLevel(VerificationLevel.FIELD);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(verificationRepository.findTopByApplicationIdOrderByVerificationDateDesc(100L))
                .thenReturn(Optional.of(lastVerification));

        VerificationResponseDto response = verificationService.resumeAfterReVerification(100L);

        assertThat(response).isNotNull();
        assertThat(response.getNewStatus()).isEqualTo(ApplicationStatus.FIELD_VERIFICATION_PENDING);
        verify(auditLogService).logEvent(eq("Application"), eq(100L), eq("RESUMED_VERIFICATION"), eq((User) null), anyString());
    }
}
