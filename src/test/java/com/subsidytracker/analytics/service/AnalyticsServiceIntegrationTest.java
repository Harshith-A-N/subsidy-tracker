package com.subsidytracker.analytics.service;

import com.subsidytracker.analytics.repository.AnalyticsRepository;
import com.subsidytracker.common.entity.*;
import com.subsidytracker.common.enums.*;
import com.subsidytracker.dashboard.dto.*;
import com.subsidytracker.disbursement.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class AnalyticsServiceIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private EntityManager entityManager;

    private Scheme scheme1;
    private Scheme scheme2;
    private RegionalBudget budget1;
    private RegionalBudget budget2;
    private User officer;
    private User beneficiaryUser1;
    private User beneficiaryUser2;
    private Beneficiary beneficiary1;
    private Beneficiary beneficiary2;
    private Application application1;
    private Application application2;
    private Verification verification1;
    private DisbursementPlan plan1;
    private DisbursementStage stage1;
    private ApplicationDisbursementSchedule schedule1;
    private DisbursementMilestone milestone1;
    private DisbursementMilestone milestone2;

    @BeforeEach
    public void setUp() {
        // 1. Schemes
        scheme1 = new Scheme();
        scheme1.setName("Solar Pump Subsidy");
        scheme1.setDescription("Solar pump subsidy scheme");
        scheme1.setTotalBudget(new BigDecimal("20000000.00"));
        scheme1.setGrantAmount(new BigDecimal("100000.00"));
        scheme1.setActive(true);
        entityManager.persist(scheme1);

        scheme2 = new Scheme();
        scheme2.setName("Rural Housing Grant");
        scheme2.setDescription("Rural housing grant scheme");
        scheme2.setTotalBudget(new BigDecimal("18000000.00"));
        scheme2.setGrantAmount(new BigDecimal("150000.00"));
        scheme2.setActive(true);
        entityManager.persist(scheme2);

        // 2. Regional Budgets
        budget1 = new RegionalBudget();
        budget1.setScheme(scheme1);
        budget1.setRegionName("Madurai");
        budget1.setAllocatedBudget(new BigDecimal("10000000.00"));
        budget1.setUtilizedBudget(BigDecimal.ZERO);
        entityManager.persist(budget1);

        budget2 = new RegionalBudget();
        budget2.setScheme(scheme1);
        budget2.setRegionName("Coimbatore");
        budget2.setAllocatedBudget(new BigDecimal("9000000.00"));
        budget2.setUtilizedBudget(BigDecimal.ZERO);
        entityManager.persist(budget2);

        // 3. Users
        officer = new User();
        officer.setFullName("Officer Madurai");
        officer.setPassword("password");
        officer.setEmail("officer@test.com");
        officer.setRole(Role.FINANCE_APPROVER);
        officer.setRegion("Madurai");
        entityManager.persist(officer);

        beneficiaryUser1 = new User();
        beneficiaryUser1.setFullName("Beneficiary One");
        beneficiaryUser1.setPassword("password");
        beneficiaryUser1.setEmail("b1@test.com");
        beneficiaryUser1.setRole(Role.BENEFICIARY);
        beneficiaryUser1.setRegion("Madurai");
        entityManager.persist(beneficiaryUser1);

        beneficiaryUser2 = new User();
        beneficiaryUser2.setFullName("Beneficiary Two");
        beneficiaryUser2.setPassword("password");
        beneficiaryUser2.setEmail("b2@test.com");
        beneficiaryUser2.setRole(Role.BENEFICIARY);
        beneficiaryUser2.setRegion("Coimbatore");
        entityManager.persist(beneficiaryUser2);

        // 4. Beneficiaries
        beneficiary1 = new Beneficiary();
        beneficiary1.setFullName("Geethika Meda");
        beneficiary1.setNationalIdNumber("NID001");
        beneficiary1.setPhoneNumber("1234567890");
        beneficiary1.setAddress("Madurai, TN");
        beneficiary1.setCategory(BeneficiaryCategory.EWS);
        beneficiary1.setRegistrationDate(LocalDate.now().minusDays(10));
        beneficiary1.setRegion("Madurai");
        beneficiary1.setAnnualIncome(new BigDecimal("120000.00"));
        beneficiary1.setUser(beneficiaryUser1);
        entityManager.persist(beneficiary1);

        beneficiary2 = new Beneficiary();
        beneficiary2.setFullName("Teammate A");
        beneficiary2.setNationalIdNumber("NID002");
        beneficiary2.setPhoneNumber("0987654321");
        beneficiary2.setAddress("Coimbatore, TN");
        beneficiary2.setCategory(BeneficiaryCategory.GENERAL);
        beneficiary2.setRegistrationDate(LocalDate.now().minusDays(5));
        beneficiary2.setRegion("Coimbatore");
        beneficiary2.setAnnualIncome(new BigDecimal("350000.00"));
        beneficiary2.setUser(beneficiaryUser2);
        entityManager.persist(beneficiary2);

        // 5. Applications
        application1 = new Application();
        application1.setBeneficiary(beneficiary1);
        application1.setScheme(scheme1);
        application1.setStatus(ApplicationStatus.READY_FOR_DISBURSEMENT);
        application1.setEligibilityScore(85.0);
        application1.setSubmissionDate(LocalDate.now().minusDays(8));
        entityManager.persist(application1);

        application2 = new Application();
        application2.setBeneficiary(beneficiary2);
        application2.setScheme(scheme1);
        application2.setStatus(ApplicationStatus.FIELD_VERIFICATION_PENDING);
        application2.setEligibilityScore(70.0);
        application2.setSubmissionDate(LocalDate.now().minusDays(4));
        entityManager.persist(application2);

        // 6. Verifications (Approval turnaround)
        verification1 = new Verification();
        verification1.setApplication(application1);
        verification1.setOfficer(officer);
        verification1.setLevel(VerificationLevel.FINANCE);
        verification1.setDecision(VerificationDecision.APPROVED);
        verification1.setRemarks("Looks good");
        verification1.setVerificationDate(LocalDateTime.now().minusDays(2)); // Turnaround = 8 - 2 = 6 days
        entityManager.persist(verification1);

        // 7. Disbursement plan
        plan1 = new DisbursementPlan();
        plan1.setScheme(scheme1);
        plan1.setNumberOfStages(2);
        plan1.setCreatedBy(officer);
        entityManager.persist(plan1);

        stage1 = new DisbursementStage();
        stage1.setPlan(plan1);
        stage1.setStageName("Initial Installment");
        stage1.setSequenceNumber(1);
        stage1.setPercentageOfGrant(new BigDecimal("50.00"));
        stage1.setTriggerMilestone(TriggerMilestone.APPLICATION_APPROVAL);
        stage1.setDueDateOffsetDays(10);
        entityManager.persist(stage1);

        // 8. Disbursement schedules
        schedule1 = new ApplicationDisbursementSchedule();
        schedule1.setApplication(application1);
        schedule1.setStage(stage1);
        schedule1.setScheduledAmount(new BigDecimal("50000.00"));
        schedule1.setDueDate(LocalDate.now().minusDays(1));
        schedule1.setStatus(DisbursementScheduleStatus.RELEASED); // Released = utilized
        entityManager.persist(schedule1);

        // 9. Compliance milestones
        milestone1 = new DisbursementMilestone();
        milestone1.setApplication(application1);
        milestone1.setStage(stage1);
        milestone1.setMilestoneType(MilestoneType.DOCUMENTATION);
        milestone1.setSequenceOrder(1);
        milestone1.setDescription("Submit paperwork");
        milestone1.setScheduledAmount(new BigDecimal("50000.00"));
        milestone1.setDueDate(LocalDate.now().minusDays(5));
        milestone1.setComplianceStatus(ComplianceStatus.COMPLETED);
        milestone1.setDisbursementStatus(DisbursementStatus.RELEASED);
        entityManager.persist(milestone1);

        milestone2 = new DisbursementMilestone();
        milestone2.setApplication(application1);
        milestone2.setStage(stage1);
        milestone2.setMilestoneType(MilestoneType.GROUND_VERIFICATION);
        milestone2.setSequenceOrder(2);
        milestone2.setDescription("Field verification check");
        milestone2.setScheduledAmount(new BigDecimal("50000.00"));
        milestone2.setDueDate(LocalDate.now().minusDays(1));
        milestone2.setComplianceStatus(ComplianceStatus.OVERDUE);
        milestone2.setDisbursementStatus(DisbursementStatus.NOT_RELEASED);
        entityManager.persist(milestone2);

        entityManager.flush();
    }

    @Test
    public void testFundUtilizationByScheme() {
        List<SchemeUtilizationDto> utilizations = analyticsService.fundUtilizationByScheme();
        assertThat(utilizations).isNotEmpty();

        // Scheme 1 should have 20000000 budget and 50000 utilized
        SchemeUtilizationDto u1 = utilizations.stream()
                .filter(u -> u.getSchemeName().equals("Solar Pump Subsidy"))
                .findFirst().orElseThrow();

        assertThat(u1.getTotalBudget()).isEqualByComparingTo("20000000.00");
        assertThat(u1.getUtilizedBudget()).isEqualByComparingTo("50000.00");
        assertThat(u1.getUtilizationPercent()).isEqualTo(0.25); // 50000 / 20000000 * 100 = 0.25%
    }

    @Test
    public void testFundUtilizationByRegion() {
        List<RegionUtilizationDto> regionalUtils = analyticsService.fundUtilizationByRegion();
        assertThat(regionalUtils).isNotEmpty();

        // Madurai should have allocated budget 10000000, utilized budget 50000, 1 application (total), 1 approved
        RegionUtilizationDto madurai = regionalUtils.stream()
                .filter(r -> r.getRegionName().equals("Madurai"))
                .findFirst().orElseThrow();

        assertThat(madurai.getAllocatedBudget()).isEqualByComparingTo("10000000.00");
        assertThat(madurai.getUtilizedBudget()).isEqualByComparingTo("50000.00");
        assertThat(madurai.getUtilizationPercent()).isEqualTo(0.5); // 50000 / 10000000 * 100 = 0.5%
        assertThat(madurai.getApplicationCount()).isEqualTo(1);
        assertThat(madurai.getApprovedCount()).isEqualTo(1);

        // Coimbatore should have allocated budget 9000000, utilized budget 0, 1 application, 0 approved
        RegionUtilizationDto coimbatore = regionalUtils.stream()
                .filter(r -> r.getRegionName().equals("Coimbatore"))
                .findFirst().orElseThrow();

        assertThat(coimbatore.getAllocatedBudget()).isEqualByComparingTo("9000000.00");
        assertThat(coimbatore.getUtilizedBudget()).isEqualByComparingTo("0.00");
        assertThat(coimbatore.getUtilizationPercent()).isEqualTo(0.0);
        assertThat(coimbatore.getApplicationCount()).isEqualTo(1);
        assertThat(coimbatore.getApprovedCount()).isEqualTo(0);
    }

    @Test
    public void testPendingMilestoneSummary() {
        PendingMilestoneSummaryDto summary = analyticsService.pendingMilestoneSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getCompletedCount()).isEqualTo(1);
        assertThat(summary.getOverdueCount()).isEqualTo(1);
        assertThat(summary.getPendingCount()).isEqualTo(0);
    }

    @Test
    public void testNonComplianceAnalysis() {
        List<NonComplianceDto> list = analyticsService.nonComplianceAnalysis();
        assertThat(list).isNotEmpty();

        NonComplianceDto nonComp = list.get(0);
        assertThat(nonComp.getSchemeName()).isEqualTo("Solar Pump Subsidy");
        assertThat(nonComp.getRegionName()).isEqualTo("Madurai");
        assertThat(nonComp.getNonCompliantCount()).isEqualTo(1);
    }

    @Test
    public void testApprovalTurnaroundTime() {
        ApprovalTurnaroundDto turnaround = analyticsService.approvalTurnaroundTime();
        assertThat(turnaround).isNotNull();
        assertThat(turnaround.getAverageDays()).isEqualTo(6.0); // 6 days turnaround
        assertThat(turnaround.getFastestDays()).isEqualTo(6.0);
        assertThat(turnaround.getSlowestDays()).isEqualTo(6.0);
    }

    @Test
    public void testBudgetExhaustionWarnings() {
        List<BudgetExhaustionWarningDto> warnings = analyticsService.budgetExhaustionWarnings();
        assertThat(warnings).isNotEmpty();

        BudgetExhaustionWarningDto w1 = warnings.stream()
                .filter(w -> w.getRegionName().equals("Madurai"))
                .findFirst().orElseThrow();

        assertThat(w1.getSchemeName()).isEqualTo("Solar Pump Subsidy");
        assertThat(w1.getUtilizationPercent()).isEqualTo(0.5);
        assertThat(w1.getSeverity()).isEqualTo("OK");
    }

    @Test
    public void testBeneficiaryCategoryDistribution() {
        List<CategoryDistributionDto> dist = analyticsService.beneficiaryCategoryDistribution();
        assertThat(dist).isNotEmpty();

        CategoryDistributionDto ews = dist.stream()
                .filter(d -> d.getCategory().equals("EWS"))
                .findFirst().orElseThrow();
        assertThat(ews.getCount()).isEqualTo(1);
        assertThat(ews.getPercent()).isEqualTo(50.0); // 1 out of 2 total applications is EWS

        CategoryDistributionDto general = dist.stream()
                .filter(d -> d.getCategory().equals("GENERAL"))
                .findFirst().orElseThrow();
        assertThat(general.getCount()).isEqualTo(1);
        assertThat(general.getPercent()).isEqualTo(50.0); // 1 out of 2 total applications is GENERAL
    }

    @Test
    public void testOverview() {
        DashboardOverviewDto overview = analyticsService.overview();
        assertThat(overview).isNotNull();
        assertThat(overview.getTotalApplications()).isEqualTo(2);
        assertThat(overview.getApprovedApplications()).isEqualTo(1);
        assertThat(overview.getPendingApplications()).isEqualTo(1);
        assertThat(overview.getRejectedApplications()).isEqualTo(0);
        assertThat(overview.getTotalBudgetAllocated()).isEqualByComparingTo("38000000.00"); // 20000000 + 18000000
        assertThat(overview.getTotalBudgetUtilized()).isEqualByComparingTo("50000.00");
        assertThat(overview.getOverdueMilestones()).isEqualTo(1);
    }
}
