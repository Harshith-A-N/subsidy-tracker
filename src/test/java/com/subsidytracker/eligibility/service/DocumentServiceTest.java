package com.subsidytracker.eligibility.service;

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
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.DocumentVerificationStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.common.service.CloudinaryService;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private DisbursementStageRepository stageRepository;
    @Mock private ApplicationDisbursementScheduleRepository scheduleRepository;
    @Mock private DisbursementMilestoneRepository milestoneRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private DocumentService documentService;

    private Application application;
    private Beneficiary beneficiary;
    private Document document;
    private User fieldOfficer;

    @BeforeEach
    void setUp() {
        beneficiary = new Beneficiary();
        beneficiary.setRegion("North");

        application = new Application();
        application.setId(100L);
        application.setBeneficiary(beneficiary);
        application.setStatus(ApplicationStatus.FIELD_VERIFICATION_PENDING);

        document = new Document();
        document.setId(200L);
        document.setApplication(application);
        document.setDocumentType("AADHAR");
        document.setVerificationStatus(DocumentVerificationStatus.PENDING);

        fieldOfficer = new User();
        fieldOfficer.setId(5L);
        fieldOfficer.setRole(Role.FIELD_OFFICER);
        fieldOfficer.setRegion("North");
    }

    @Test
    void verifyDocument_ShouldSucceed_ForFieldOfficerInRegionDuringFieldReview() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(documentRepository.findById(200L)).thenReturn(Optional.of(document));
        when(userRepository.findById(5L)).thenReturn(Optional.of(fieldOfficer));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponseDto response = documentService.verifyDocument(
                100L, 200L, DocumentVerificationStatus.VERIFIED, "Looks genuine", 5L);

        assertThat(response.getVerificationStatus()).isEqualTo(DocumentVerificationStatus.VERIFIED);
        verify(documentRepository).save(any(Document.class));
        verify(auditLogService).logEvent(eq("Document"), eq(200L), eq("DOCUMENT_VERIFIED"), eq(fieldOfficer), anyString());
    }

    @Test
    void verifyDocument_ShouldRejectIdorAttempt_WhenDocumentBelongsToDifferentApplication() {
        Application otherApplication = new Application();
        otherApplication.setId(999L);
        document.setApplication(otherApplication);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(documentRepository.findById(200L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.verifyDocument(
                100L, 200L, DocumentVerificationStatus.VERIFIED, "remarks", 5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void verifyDocument_ShouldReject_WhenOfficerRegionDoesNotMatchBeneficiary() {
        fieldOfficer.setRegion("South");

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(documentRepository.findById(200L)).thenReturn(Optional.of(document));
        when(userRepository.findById(5L)).thenReturn(Optional.of(fieldOfficer));

        assertThatThrownBy(() -> documentService.verifyDocument(
                100L, 200L, DocumentVerificationStatus.VERIFIED, "remarks", 5L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not in your assigned region");

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void verifyDocument_ShouldReject_WhenApplicationNotAtFieldReview() {
        application.setStatus(ApplicationStatus.DISTRICT_REVIEW_PENDING);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(documentRepository.findById(200L)).thenReturn(Optional.of(document));
        when(userRepository.findById(5L)).thenReturn(Optional.of(fieldOfficer));

        assertThatThrownBy(() -> documentService.verifyDocument(
                100L, 200L, DocumentVerificationStatus.VERIFIED, "remarks", 5L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pending Field review");

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void verifyDocument_ShouldReject_ForNonFieldOfficerNonAdminRole() {
        User districtOfficer = new User();
        districtOfficer.setId(6L);
        districtOfficer.setRole(Role.DISTRICT_OFFICER);
        districtOfficer.setRegion("North");

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(documentRepository.findById(200L)).thenReturn(Optional.of(document));
        when(userRepository.findById(6L)).thenReturn(Optional.of(districtOfficer));

        assertThatThrownBy(() -> documentService.verifyDocument(
                100L, 200L, DocumentVerificationStatus.VERIFIED, "remarks", 6L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Field Officer");

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void verifyDocument_ShouldReject_ForStageLinkedUtilizationProof() {
        com.subsidytracker.disbursement.entity.DisbursementStage stage =
                new com.subsidytracker.disbursement.entity.DisbursementStage();
        stage.setId(50L);
        document.setStage(stage);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(documentRepository.findById(200L)).thenReturn(Optional.of(document));
        when(userRepository.findById(5L)).thenReturn(Optional.of(fieldOfficer));

        assertThatThrownBy(() -> documentService.verifyDocument(
                100L, 200L, DocumentVerificationStatus.VERIFIED, "remarks", 5L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("KYC documents only");

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void verifyDocument_ShouldSucceed_ForAdminRegardlessOfStageOrRegion() {
        User admin = new User();
        admin.setId(7L);
        admin.setRole(Role.ADMIN);
        admin.setRegion("SomeOtherRegion");

        application.setStatus(ApplicationStatus.DISTRICT_REVIEW_PENDING);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(documentRepository.findById(200L)).thenReturn(Optional.of(document));
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponseDto response = documentService.verifyDocument(
                100L, 200L, DocumentVerificationStatus.REJECTED, "Bad scan", 7L);

        assertThat(response.getVerificationStatus()).isEqualTo(DocumentVerificationStatus.REJECTED);
        verify(documentRepository).save(any(Document.class));
    }
}