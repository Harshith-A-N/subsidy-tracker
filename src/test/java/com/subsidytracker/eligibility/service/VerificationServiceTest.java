package com.subsidytracker.eligibility.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.Document;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.entity.Verification;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.enums.VerificationDecision;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.disbursement.service.ScheduleGenerationService;
import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private VerificationRepository verificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SchemeSlabRepository schemeSlabRepository;
    @Mock private ScheduleGenerationService scheduleGenerationService;
    @Mock private AuditLogService auditLogService;
    @Mock private DocumentRepository documentRepository;

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

        Document verifiedDoc = new Document();
        verifiedDoc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        when(documentRepository.findByApplicationIdAndStageIsNull(100L))
                .thenReturn(List.of(verifiedDoc));
        when(documentRepository.existsByApplicationIdAndStageIsNullAndVerificationStatusNot(
                100L, DocumentVerificationStatus.VERIFIED))
                .thenReturn(false);

        VerificationResponseDto response = verificationService.processVerification(100L, request, 5L);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationId()).isEqualTo(100L);
        verify(verificationRepository).save(any(Verification.class));
        verify(auditLogService).logEvent(eq("Application"), eq(100L), eq("APPROVED"), eq(officer), anyString());
    }

    @Test
    void processVerification_ShouldRejectFieldApproval_WhenDocumentsNotVerified() {
        VerificationRequestDto request = new VerificationRequestDto();
        request.setDecision(VerificationDecision.APPROVED);
        request.setRemarks("Looks good");

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(userRepository.findById(5L)).thenReturn(Optional.of(officer));

        Document pendingDoc = new Document();
        pendingDoc.setVerificationStatus(DocumentVerificationStatus.PENDING);
        when(documentRepository.findByApplicationIdAndStageIsNull(100L))
                .thenReturn(List.of(pendingDoc));
        when(documentRepository.existsByApplicationIdAndStageIsNullAndVerificationStatusNot(
                100L, DocumentVerificationStatus.VERIFIED))
                .thenReturn(true);

        assertThatThrownBy(() -> verificationService.processVerification(100L, request, 5L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not been marked VERIFIED");

        verify(verificationRepository, never()).save(any(Verification.class));
        verify(applicationRepository, never()).save(any(Application.class));
    }
}