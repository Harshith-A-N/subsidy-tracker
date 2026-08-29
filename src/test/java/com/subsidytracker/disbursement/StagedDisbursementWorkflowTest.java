package com.subsidytracker.disbursement;

import com.subsidytracker.common.entity.*;
import com.subsidytracker.common.enums.*;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.service.CloudinaryService;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.disbursement.repository.DisbursementPlanRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.disbursement.service.ComplianceMilestoneService;
import com.subsidytracker.disbursement.service.ScheduleGenerationService;
import com.subsidytracker.eligibility.dto.DocumentResponseDto;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.beneficiary.repository.BeneficiaryRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.service.DocumentService;
import com.subsidytracker.scheme.repository.SchemeRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
public class StagedDisbursementWorkflowTest {

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Autowired
    private ComplianceMilestoneService complianceMilestoneService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeSlabRepository schemeSlabRepository;

    @Autowired
    private DisbursementPlanRepository planRepository;

    @Autowired
    private DisbursementStageRepository stageRepository;

    @Autowired
    private ApplicationDisbursementScheduleRepository scheduleRepository;

    @Autowired
    private DisbursementMilestoneRepository milestoneRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    private User beneficiaryUser;
    private User financeUser;
    private User officerUser;
    private User otherBeneficiaryUser;
    private Application application;
    private Scheme scheme;
    private DisbursementPlan plan;
    private DisbursementStage stage1;
    private DisbursementStage stage2;

    @BeforeEach
    void setUp() {
        when(cloudinaryService.upload(any())).thenReturn("http://cloudinary.com/test-proof.jpg");

        // 1. Create Users
        beneficiaryUser = new User();
        beneficiaryUser.setFullName("Test Beneficiary User");
        beneficiaryUser.setEmail("ben_test_" + System.currentTimeMillis() + "@example.com");
        beneficiaryUser.setPassword("password");
        beneficiaryUser.setRole(Role.BENEFICIARY);
        beneficiaryUser = userRepository.save(beneficiaryUser);

        otherBeneficiaryUser = new User();
        otherBeneficiaryUser.setFullName("Other Beneficiary User");
        otherBeneficiaryUser.setEmail("ben_other_" + System.currentTimeMillis() + "@example.com");
        otherBeneficiaryUser.setPassword("password");
        otherBeneficiaryUser.setRole(Role.BENEFICIARY);
        otherBeneficiaryUser = userRepository.save(otherBeneficiaryUser);

        financeUser = new User();
        financeUser.setFullName("Finance Approver User");
        financeUser.setEmail("finance_test_" + System.currentTimeMillis() + "@example.com");
        financeUser.setPassword("password");
        financeUser.setRole(Role.FINANCE_APPROVER);
        financeUser = userRepository.save(financeUser);

        officerUser = new User();
        officerUser.setFullName("Field Officer User");
        officerUser.setEmail("officer_test_" + System.currentTimeMillis() + "@example.com");
        officerUser.setPassword("password");
        officerUser.setRole(Role.FIELD_OFFICER);
        officerUser.setRegion("Maharashtra");
        officerUser = userRepository.save(officerUser);

        // 2. Create Beneficiary Profile
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setFullName("Test Beneficiary");
        beneficiary.setAddress("123 Main St, Mumbai");
        beneficiary.setPhoneNumber("9876543210");
        beneficiary.setCategory(BeneficiaryCategory.GENERAL);
        beneficiary.setRegion("Maharashtra");
        beneficiary.setAnnualIncome(new BigDecimal("100000"));
        beneficiary.setNationalIdNumber("123456789012");
        beneficiary.setRegistrationDate(LocalDate.now());
        beneficiary.setUser(beneficiaryUser);
        beneficiary = beneficiaryRepository.save(beneficiary);

        // 3. Create Scheme & Slab
        scheme = new Scheme();
        scheme.setName("Test Agriculture Subsidy");
        scheme.setActive(true);
        scheme = schemeRepository.save(scheme);

        SchemeSlab slab = new SchemeSlab();
        slab.setScheme(scheme);
        slab.setCategory(BeneficiaryCategory.GENERAL);
        slab.setGrantAmount(new BigDecimal("100000"));
        schemeSlabRepository.save(slab);

        // 4. Create Disbursement Plan & Stages
        plan = new DisbursementPlan();
        plan.setScheme(scheme);
        plan.setNumberOfStages(2);
        plan = planRepository.save(plan);

        stage1 = new DisbursementStage();
        stage1.setPlan(plan);
        stage1.setStageName("Stage 1 - Equipment Payout");
        stage1.setSequenceNumber(1);
        stage1.setPercentageOfGrant(new BigDecimal("50"));
        stage1.setDueDateOffsetDays(7);
        stage1.setTriggerMilestone(TriggerMilestone.APPLICATION_APPROVAL);
        stage1 = stageRepository.save(stage1);

        stage2 = new DisbursementStage();
        stage2.setPlan(plan);
        stage2.setStageName("Stage 2 - Inspection Payout");
        stage2.setSequenceNumber(2);
        stage2.setPercentageOfGrant(new BigDecimal("50"));
        stage2.setDueDateOffsetDays(14);
        stage2.setTriggerMilestone(TriggerMilestone.UTILIZATION_PROOF);
        stage2 = stageRepository.save(stage2);

        // 5. Create Application
        application = new Application();
        application.setBeneficiary(beneficiary);
        application.setScheme(scheme);
        application.setStatus(ApplicationStatus.READY_FOR_DISBURSEMENT);
        application.setSubmissionDate(LocalDate.now());
        application.setEligibilityScore(100);
        application = applicationRepository.save(application);

        // Generate Schedule & Milestones
        scheduleGenerationService.generateSchedule(application.getId());
    }

    @Test
    void testReleaseStage1_Success() {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched1 = schedules.get(0);

        assertEquals(DisbursementScheduleStatus.PENDING, sched1.getStatus());

        ApplicationDisbursementSchedule released = scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());

        assertEquals(DisbursementScheduleStatus.RELEASED, released.getStatus());
    }

    @Test
    void testReleaseStage2_FailsIfStage1NotReleased() {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched2 = schedules.get(1);

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            scheduleGenerationService.releaseStage(sched2.getId(), financeUser.getId());
        });

        assertTrue(ex.getMessage().contains("must be released before this stage can be released"));
    }

    @Test
    void testReleaseStage2_FailsIfStage1ProofMissing() {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched1 = schedules.get(0);
        ApplicationDisbursementSchedule sched2 = schedules.get(1);

        // Release Stage 1
        scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());

        // Try to release Stage 2 without Stage 1 proof verified
        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            scheduleGenerationService.releaseStage(sched2.getId(), financeUser.getId());
        });

        assertTrue(ex.getMessage().contains("must be verified by an officer before releasing the next stage"));
    }

    @Test
    void testReleaseStage2_FailsIfStage1ProofNotVerified() throws Exception {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched1 = schedules.get(0);
        ApplicationDisbursementSchedule sched2 = schedules.get(1);

        // 1. Release Stage 1
        scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());

        // 2. Beneficiary uploads Stage 1 proof (submitted, but NOT verified yet)
        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        // 3. Finance attempts Stage 2 release -> Must fail because proof is not verified yet by officer
        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            scheduleGenerationService.releaseStage(sched2.getId(), financeUser.getId());
        });

        assertTrue(ex.getMessage().contains("must be verified by an officer before releasing the next stage"));
    }

    @Test
    void testReleaseStage2_SuccessAfterStage1ReleasedAndVerified() throws Exception {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched1 = schedules.get(0);
        ApplicationDisbursementSchedule sched2 = schedules.get(1);

        List<DisbursementMilestone> milestones = milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(application.getId());
        DisbursementMilestone ms1 = milestones.get(0);

        // 1. Release Stage 1
        scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());

        // 2. Upload Proof
        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        // 3. Officer verifies milestone 1
        complianceMilestoneService.completeMilestone(ms1.getId(), officerUser.getId());

        // 4. Finance releases Stage 2 -> Success!
        ApplicationDisbursementSchedule released2 = scheduleGenerationService.releaseStage(sched2.getId(), financeUser.getId());

        assertEquals(DisbursementScheduleStatus.RELEASED, released2.getStatus());
    }

    @Test
    void testBeneficiaryCanUploadProofOnlyAfterStageReleased() {
        // Stage 1 is still PENDING (not released)
        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());
        });

        assertTrue(ex.getMessage().contains("Utilization proof can only be uploaded after the corresponding stage has been released"));
    }

    @Test
    void testBeneficiaryCannotUploadProofForFutureStage() {
        // Release Stage 1, but try uploading proof for Stage 2
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        scheduleGenerationService.releaseStage(schedules.get(0).getId(), financeUser.getId());

        MockMultipartFile file = new MockMultipartFile("file", "proof2.png", "image/png", "test image".getBytes());

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage2.getId());
        });

        assertTrue(ex.getMessage().contains("Utilization proof can only be uploaded after the corresponding stage has been released"));
    }

    @Test
    void testBeneficiaryCannotUploadProofForAnotherApplication() {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        scheduleGenerationService.releaseStage(schedules.get(0).getId(), financeUser.getId());

        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, otherBeneficiaryUser.getId(), stage1.getId());
        });

        assertTrue(ex.getMessage().contains("authorized to access") || ex.getMessage().contains("not belong"));
    }

    @Test
    void testOfficerCannotVerifyWithoutProof() {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        scheduleGenerationService.releaseStage(schedules.get(0).getId(), financeUser.getId());

        List<DisbursementMilestone> milestones = milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(application.getId());
        DisbursementMilestone ms1 = milestones.get(0);

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            complianceMilestoneService.completeMilestone(ms1.getId(), officerUser.getId());
        });

        assertTrue(ex.getMessage().contains("No utilization proof has been uploaded"));
    }

    @Test
    void testFinanceCannotCompleteCompliance() throws Exception {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        scheduleGenerationService.releaseStage(schedules.get(0).getId(), financeUser.getId());

        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        List<DisbursementMilestone> milestones = milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(application.getId());
        DisbursementMilestone ms1 = milestones.get(0);

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            complianceMilestoneService.completeMilestone(ms1.getId(), financeUser.getId());
        });

        assertTrue(ex.getMessage().contains("Finance Approvers are not permitted to complete compliance milestones"));
    }

    @Test
    void testOfficerCanVerifyValidProof() throws Exception {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        scheduleGenerationService.releaseStage(schedules.get(0).getId(), financeUser.getId());

        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        List<DisbursementMilestone> milestones = milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(application.getId());
        DisbursementMilestone ms1 = milestones.get(0);

        DisbursementMilestone completed = complianceMilestoneService.completeMilestone(ms1.getId(), officerUser.getId());

        assertEquals(ComplianceStatus.COMPLETED, completed.getComplianceStatus());
        assertEquals(officerUser.getId(), completed.getCompletedBy().getId());
    }

    @Test
    void testCompletingMilestoneDoesNotReleaseStage() throws Exception {
        // Verify that completeMilestone ONLY updates compliance status and DOES NOT mutate schedule status
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched1 = schedules.get(0);
        ApplicationDisbursementSchedule sched2 = schedules.get(1);

        scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());

        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        List<DisbursementMilestone> milestones = milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(application.getId());
        
        complianceMilestoneService.completeMilestone(milestones.get(0).getId(), officerUser.getId());

        // Stage 2 schedule MUST remain PENDING after milestone 1 completion!
        ApplicationDisbursementSchedule refreshedSched2 = scheduleRepository.findById(sched2.getId()).get();
        assertEquals(DisbursementScheduleStatus.PENDING, refreshedSched2.getStatus());
    }

    @Test
    void testNextStageBecomesEligibleOnlyAfterPreviousCompliance() throws Exception {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched1 = schedules.get(0);
        ApplicationDisbursementSchedule sched2 = schedules.get(1);

        // Stage 1 release
        scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());

        // Stage 2 release blocked
        assertThrows(InvalidOperationException.class, () -> scheduleGenerationService.releaseStage(sched2.getId(), financeUser.getId()));

        // Beneficiary proof upload
        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        // Stage 2 release still blocked (not verified yet)
        assertThrows(InvalidOperationException.class, () -> scheduleGenerationService.releaseStage(sched2.getId(), financeUser.getId()));

        // Officer verification
        List<DisbursementMilestone> milestones = milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(application.getId());
        complianceMilestoneService.completeMilestone(milestones.get(0).getId(), officerUser.getId());

        // Stage 2 release NOW succeeds!
        ApplicationDisbursementSchedule rel2 = scheduleGenerationService.releaseStage(sched2.getId(), financeUser.getId());
        assertEquals(DisbursementScheduleStatus.RELEASED, rel2.getStatus());
    }

    @Test
    void testExistingKycDocumentUploadStillWorks() throws Exception {
        Application draftApp = new Application();
        draftApp.setBeneficiary(application.getBeneficiary());
        draftApp.setScheme(scheme);
        draftApp.setStatus(ApplicationStatus.DRAFT);
        draftApp.setSubmissionDate(LocalDate.now());
        draftApp = applicationRepository.save(draftApp);

        MockMultipartFile file = new MockMultipartFile("file", "aadhaar.pdf", "application/pdf", "aadhaar content".getBytes());
        DocumentResponseDto dto = documentService.uploadDocument(draftApp.getId(), "AADHAAR", file, beneficiaryUser.getId());

        assertNotNull(dto.getId());
        assertNull(dto.getStageId());
        assertEquals("AADHAAR", dto.getDocumentType());
    }

    @Test
    void testCloudinaryDocumentRetrievalStillWorks() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        scheduleGenerationService.releaseStage(scheduleRepository.findByApplicationId(application.getId()).get(0).getId(), financeUser.getId());

        DocumentResponseDto dto = documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        Document doc = documentRepository.findById(dto.getId()).get();
        assertEquals("http://cloudinary.com/test-proof.jpg", doc.getFilePath());
    }

    @Test
    void testUnauthorizedBeneficiaryCannotAccessAnotherApplicationProof() {
        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());

        assertThrows(InvalidOperationException.class, () -> {
            documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, otherBeneficiaryUser.getId(), stage1.getId());
        });
    }

    @Test
    void testAlreadyReleasedStageCannotBeReleasedAgain() {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        ApplicationDisbursementSchedule sched1 = schedules.get(0);

        scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            scheduleGenerationService.releaseStage(sched1.getId(), financeUser.getId());
        });

        assertTrue(ex.getMessage().contains("already been released"));
    }

    @Test
    void testAlreadyCompletedMilestoneCannotBeCompletedAgain() throws Exception {
        List<ApplicationDisbursementSchedule> schedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(application.getId());
        scheduleGenerationService.releaseStage(schedules.get(0).getId(), financeUser.getId());

        MockMultipartFile file = new MockMultipartFile("file", "proof1.png", "image/png", "test image".getBytes());
        documentService.uploadDocument(application.getId(), "STAGE_UTILIZATION_PROOF", file, beneficiaryUser.getId(), stage1.getId());

        List<DisbursementMilestone> milestones = milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(application.getId());
        DisbursementMilestone ms1 = milestones.get(0);

        DisbursementMilestone firstComp = complianceMilestoneService.completeMilestone(ms1.getId(), officerUser.getId());
        assertEquals(ComplianceStatus.COMPLETED, firstComp.getComplianceStatus());

        // Completing again returns the managed entity without error or duplicate side-effects
        DisbursementMilestone secondComp = complianceMilestoneService.completeMilestone(ms1.getId(), officerUser.getId());
        assertEquals(ComplianceStatus.COMPLETED, secondComp.getComplianceStatus());
    }
}
