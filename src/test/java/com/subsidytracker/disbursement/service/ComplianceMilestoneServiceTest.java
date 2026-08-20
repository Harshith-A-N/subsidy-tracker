package com.subsidytracker.disbursement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.RegionalBudget;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import com.subsidytracker.common.enums.DisbursementStatus;
import com.subsidytracker.common.enums.TriggerMilestone;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.common.service.AuditLogService;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.scheme.repository.RegionalBudgetRepository;

@ExtendWith(MockitoExtension.class)
class ComplianceMilestoneServiceTest {

    @Mock private DisbursementMilestoneRepository milestoneRepository;
    @Mock private ApplicationDisbursementScheduleRepository scheduleRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private RegionalBudgetRepository regionalBudgetRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ComplianceMilestoneService complianceMilestoneService;

    private Application application;
    private Scheme scheme;
    private Beneficiary beneficiary;

    @BeforeEach
    void setUp() {
        scheme = new Scheme();
        scheme.setId(1L);
        scheme.setName("Solar Pump Subsidy");

        beneficiary = new Beneficiary();
        beneficiary.setId(20L);
        beneficiary.setRegion("Madurai");

        application = new Application();
        application.setId(100L);
        application.setScheme(scheme);
        application.setBeneficiary(beneficiary);
        application.setStatus(ApplicationStatus.READY_FOR_DISBURSEMENT);
    }

    // ======================== createMilestones ========================

    @Test
    void createMilestones_throwsWhenApplicationNotFound() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceMilestoneService.createMilestones(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createMilestones_throwsWhenNoScheduleExists() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(scheduleRepository.findByApplicationIdOrderByStageSequenceNumberAsc(100L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> complianceMilestoneService.createMilestones(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("No disbursement schedule exists");
    }

    @Test
    void createMilestones_isIdempotent_returnsExistingWithoutRebuilding() {
        List<DisbursementMilestone> existing = List.of(new DisbursementMilestone());

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(scheduleRepository.findByApplicationIdOrderByStageSequenceNumberAsc(100L))
                .thenReturn(List.of(schedule(stage("Stage 1", 1, TriggerMilestone.APPLICATION_APPROVAL),
                        new BigDecimal("1000"), LocalDate.now(), DisbursementScheduleStatus.PENDING)));
        when(milestoneRepository.findByApplicationIdOrderBySequenceOrderAsc(100L))
                .thenReturn(existing);

        List<DisbursementMilestone> result = complianceMilestoneService.createMilestones(100L);

        assertThat(result).isSameAs(existing);
        verify(milestoneRepository, never()).saveAll(any());
    }

    @Test
    void createMilestones_buildsOneMilestonePerScheduleEntry_withDueDateCopiedFromSchedule() {
        DisbursementStage stage1 = stage("Documentation", 1, TriggerMilestone.APPLICATION_APPROVAL);
        DisbursementStage stage2 = stage("Ground Verification", 2, TriggerMilestone.GROUND_VERIFICATION);

        LocalDate stage1Due = LocalDate.now();
        LocalDate stage2Due = LocalDate.now().plusDays(7);

        ApplicationDisbursementSchedule s1 = schedule(stage1, new BigDecimal("2500"), stage1Due, DisbursementScheduleStatus.PENDING);
        ApplicationDisbursementSchedule s2 = schedule(stage2, new BigDecimal("3500"), stage2Due, DisbursementScheduleStatus.PENDING);
        s1.setApplication(application);
        s2.setApplication(application);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(scheduleRepository.findByApplicationIdOrderByStageSequenceNumberAsc(100L))
                .thenReturn(List.of(s1, s2));
        when(milestoneRepository.findByApplicationIdOrderBySequenceOrderAsc(100L))
                .thenReturn(List.of());
        when(milestoneRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<DisbursementMilestone> result = complianceMilestoneService.createMilestones(100L);

        assertThat(result).hasSize(2);
        // Milestone due dates must come straight from their schedule entry —
        // this is what keeps /schedules and the milestone tracker consistent.
        assertThat(result.get(0).getDueDate()).isEqualTo(stage1Due);
        assertThat(result.get(1).getDueDate()).isEqualTo(stage2Due);
        assertThat(result.get(0).getComplianceStatus()).isEqualTo(ComplianceStatus.PENDING);
        assertThat(result.get(0).getDisbursementStatus()).isEqualTo(DisbursementStatus.NOT_RELEASED);
    }

    // ======================== completeMilestone ========================

    @Test
    void completeMilestone_throwsWhenMilestoneNotFound() {
        when(milestoneRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceMilestoneService.completeMilestone(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeMilestone_alreadyCompleted_isANoOpAndDoesNotReReleaseFunds() {
        DisbursementMilestone milestone = milestone(
                stage("Stage 1", 1, TriggerMilestone.APPLICATION_APPROVAL),
                new BigDecimal("1000"), ComplianceStatus.COMPLETED);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        DisbursementMilestone result = complianceMilestoneService.completeMilestone(1L);

        assertThat(result).isSameAs(milestone);
        verify(scheduleRepository, never()).findByApplicationId(any());
        verify(milestoneRepository, never()).save(any());
    }

    @Test
    void completeMilestone_releasesMatchingScheduleAndMarksCompliant() {
        DisbursementStage stage = stage("Ground Verification", 1, TriggerMilestone.GROUND_VERIFICATION);
        stage.setId(50L);

        DisbursementMilestone milestone = milestone(stage, new BigDecimal("3000"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule matchingSchedule =
                schedule(stage, new BigDecimal("3000"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        matchingSchedule.setApplication(application);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(matchingSchedule));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of());
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DisbursementMilestone result = complianceMilestoneService.completeMilestone(1L);

        assertThat(result.getComplianceStatus()).isEqualTo(ComplianceStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(matchingSchedule.getStatus()).isEqualTo(DisbursementScheduleStatus.RELEASED);
        verify(scheduleRepository).save(matchingSchedule);
    }

    @Test
    void completeMilestone_setsCompletedByActor() {
        DisbursementStage stage = stage("Ground Verification", 1, TriggerMilestone.GROUND_VERIFICATION);
        stage.setId(50L);

        DisbursementMilestone milestone = milestone(stage, new BigDecimal("3000"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule matchingSchedule =
                schedule(stage, new BigDecimal("3000"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        matchingSchedule.setApplication(application);

        User officer = new User();
        officer.setId(10L);
        officer.setEmail("officer@test.com");

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(userRepository.findById(10L)).thenReturn(Optional.of(officer));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(matchingSchedule));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of());
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DisbursementMilestone result = complianceMilestoneService.completeMilestone(1L, 10L);

        assertThat(result.getComplianceStatus()).isEqualTo(ComplianceStatus.COMPLETED);
        assertThat(result.getCompletedBy()).isEqualTo(officer);
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    void completeMilestone_updatesRegionalBudgetUtilization_whenMatchingBudgetExists() {
        DisbursementStage stage = stage("Ground Verification", 1, TriggerMilestone.GROUND_VERIFICATION);
        stage.setId(50L);

        DisbursementMilestone milestone = milestone(stage, new BigDecimal("3000"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule matchingSchedule =
                schedule(stage, new BigDecimal("3000"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        matchingSchedule.setApplication(application);

        RegionalBudget budget = new RegionalBudget();
        budget.setScheme(scheme);
        budget.setRegionName("Madurai");
        budget.setAllocatedBudget(new BigDecimal("10000000"));
        budget.setUtilizedBudget(new BigDecimal("500000"));

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(matchingSchedule));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of(budget));
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        complianceMilestoneService.completeMilestone(1L);

        assertThat(budget.getUtilizedBudget()).isEqualByComparingTo(new BigDecimal("503000"));
        verify(regionalBudgetRepository).save(budget);
    }

    @Test
    void completeMilestone_doesNotFailWhenNoMatchingRegionalBudgetExists() {
        // A missing RegionalBudget row is a configuration gap (nothing was ever
        // allocated for this scheme+region), not a reason to block the release.
        DisbursementStage stage = stage("Ground Verification", 1, TriggerMilestone.GROUND_VERIFICATION);
        stage.setId(50L);

        DisbursementMilestone milestone = milestone(stage, new BigDecimal("3000"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule matchingSchedule =
                schedule(stage, new BigDecimal("3000"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        matchingSchedule.setApplication(application);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(matchingSchedule));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of()); // no budget configured
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        complianceMilestoneService.completeMilestone(1L);

        verify(regionalBudgetRepository, never()).save(any());
    }

    @Test
    void completeMilestone_leavesApplicationStatusUnchanged_whenOtherStagesStillPending() {
        DisbursementStage stage1 = stage("Documentation", 1, TriggerMilestone.APPLICATION_APPROVAL);
        stage1.setId(50L);
        DisbursementStage stage2 = stage("Ground Verification", 2, TriggerMilestone.GROUND_VERIFICATION);
        stage2.setId(51L);

        DisbursementMilestone milestone = milestone(stage1, new BigDecimal("2500"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule releasedNow =
                schedule(stage1, new BigDecimal("2500"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        releasedNow.setApplication(application);
        ApplicationDisbursementSchedule stillPending =
                schedule(stage2, new BigDecimal("3500"), LocalDate.now().plusDays(7), DisbursementScheduleStatus.PENDING);
        stillPending.setApplication(application);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(releasedNow, stillPending));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of());
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        complianceMilestoneService.completeMilestone(1L);

        // Not every stage is released yet, so the application must stay put.
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.READY_FOR_DISBURSEMENT);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void completeMilestone_advancesApplicationToDisbursed_whenAllStagesReleased_andLastStageIsNotClosure() {
        DisbursementStage onlyStage = stage("Utilization Proof", 1, TriggerMilestone.UTILIZATION_PROOF);
        onlyStage.setId(50L);

        DisbursementMilestone milestone = milestone(onlyStage, new BigDecimal("5000"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule onlySchedule =
                schedule(onlyStage, new BigDecimal("5000"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        onlySchedule.setApplication(application);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(onlySchedule));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of());
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        complianceMilestoneService.completeMilestone(1L);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DISBURSED);
        verify(applicationRepository).save(application);
    }

    @Test
    void completeMilestone_advancesApplicationToCompleted_whenFinalStageIsProjectClosure() {
        DisbursementStage closureStage = stage("Project Closure", 4, TriggerMilestone.PROJECT_CLOSURE);
        closureStage.setId(54L);

        DisbursementMilestone milestone = milestone(closureStage, new BigDecimal("1000"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule closureSchedule =
                schedule(closureStage, new BigDecimal("1000"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        closureSchedule.setApplication(application);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(closureSchedule));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of());
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        complianceMilestoneService.completeMilestone(1L);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        verify(applicationRepository).save(application);
    }

    @Test
    void completeMilestone_neverRegressesAnAlreadyCompletedApplication() {
        application.setStatus(ApplicationStatus.COMPLETED);

        DisbursementStage stage = stage("Late Cleanup", 1, TriggerMilestone.UTILIZATION_PROOF);
        stage.setId(50L);

        DisbursementMilestone milestone = milestone(stage, new BigDecimal("500"), ComplianceStatus.PENDING);
        milestone.setApplication(application);

        ApplicationDisbursementSchedule matchingSchedule =
                schedule(stage, new BigDecimal("500"), LocalDate.now(), DisbursementScheduleStatus.PENDING);
        matchingSchedule.setApplication(application);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));
        when(scheduleRepository.findByApplicationId(100L)).thenReturn(List.of(matchingSchedule));
        when(regionalBudgetRepository.findBySchemeId(1L)).thenReturn(List.of());
        when(milestoneRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        complianceMilestoneService.completeMilestone(1L);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        verify(applicationRepository, never()).save(any());
    }

    // ======================== read-only accessors ========================

    @Test
    void getPendingMilestones_delegatesToRepository() {
        List<DisbursementMilestone> pending = List.of(new DisbursementMilestone());
        when(milestoneRepository.findByComplianceStatus(ComplianceStatus.PENDING)).thenReturn(pending);

        assertThat(complianceMilestoneService.getPendingMilestones()).isSameAs(pending);
    }

    @Test
    void getOverdueMilestones_delegatesToRepository() {
        List<DisbursementMilestone> overdue = List.of(new DisbursementMilestone());
        when(milestoneRepository.findByComplianceStatus(ComplianceStatus.OVERDUE)).thenReturn(overdue);

        assertThat(complianceMilestoneService.getOverdueMilestones()).isSameAs(overdue);
    }

    @Test
    void getApplicationMilestones_delegatesToRepository() {
        List<DisbursementMilestone> milestones = List.of(new DisbursementMilestone());
        when(milestoneRepository.findByApplicationIdOrderBySequenceOrderAsc(100L)).thenReturn(milestones);

        assertThat(complianceMilestoneService.getApplicationMilestones(100L)).isSameAs(milestones);
    }

    // ======================== flagOverdueMilestones (daily cron) ========================

    @Test
    void flagOverdueMilestones_marksPendingPastDueMilestonesAsOverdue() {
        DisbursementMilestone overdue1 = milestone(
                stage("Stage 1", 1, TriggerMilestone.APPLICATION_APPROVAL),
                new BigDecimal("1000"), ComplianceStatus.PENDING);
        DisbursementMilestone overdue2 = milestone(
                stage("Stage 2", 2, TriggerMilestone.GROUND_VERIFICATION),
                new BigDecimal("2000"), ComplianceStatus.PENDING);
        overdue1.setApplication(application);
        overdue2.setApplication(application);

        when(milestoneRepository.findByComplianceStatusAndDueDateBefore(any(), any()))
                .thenReturn(List.of(overdue1, overdue2));

        complianceMilestoneService.flagOverdueMilestones();

        assertThat(overdue1.getComplianceStatus()).isEqualTo(ComplianceStatus.OVERDUE);
        assertThat(overdue2.getComplianceStatus()).isEqualTo(ComplianceStatus.OVERDUE);
        verify(milestoneRepository, times(2)).save(any());
    }

    @Test
    void flagOverdueMilestones_doesNothingWhenNoCandidates() {
        when(milestoneRepository.findByComplianceStatusAndDueDateBefore(any(), any()))
                .thenReturn(List.of());

        complianceMilestoneService.flagOverdueMilestones();

        verify(milestoneRepository, never()).save(any());
    }

    // ======================== Helpers ========================

    private DisbursementStage stage(String name, int sequence, TriggerMilestone trigger) {
        DisbursementStage stage = new DisbursementStage();
        stage.setStageName(name);
        stage.setSequenceNumber(sequence);
        stage.setTriggerMilestone(trigger);
        return stage;
    }

    private ApplicationDisbursementSchedule schedule(DisbursementStage stage, BigDecimal amount,
                                                       LocalDate dueDate, DisbursementScheduleStatus status) {
        ApplicationDisbursementSchedule schedule = new ApplicationDisbursementSchedule();
        schedule.setStage(stage);
        schedule.setScheduledAmount(amount);
        schedule.setDueDate(dueDate);
        schedule.setStatus(status);
        return schedule;
    }

    private DisbursementMilestone milestone(DisbursementStage stage, BigDecimal amount, ComplianceStatus status) {
        DisbursementMilestone milestone = new DisbursementMilestone();
        milestone.setId(1L);
        milestone.setStage(stage);
        milestone.setScheduledAmount(amount);
        milestone.setComplianceStatus(status);
        milestone.setDisbursementStatus(DisbursementStatus.NOT_RELEASED);
        milestone.setDueDate(LocalDate.now());
        milestone.setSequenceOrder(stage.getSequenceNumber());
        return milestone;
    }
}