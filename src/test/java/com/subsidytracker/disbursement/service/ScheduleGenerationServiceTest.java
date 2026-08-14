package com.subsidytracker.disbursement.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import com.subsidytracker.common.enums.TriggerMilestone;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementPlanRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleGenerationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private DisbursementPlanRepository planRepository;
    @Mock private DisbursementStageRepository stageRepository;
    @Mock private ApplicationDisbursementScheduleRepository scheduleRepository;
    @Mock private com.subsidytracker.scheme.repository.SchemeSlabRepository schemeSlabRepository;

    @InjectMocks
    private ScheduleGenerationService scheduleGenerationService;

    private Application application;
    private Scheme scheme;
    private DisbursementPlan plan;
    private com.subsidytracker.common.entity.Beneficiary beneficiary;
    private com.subsidytracker.common.entity.SchemeSlab slab;

    @BeforeEach
    void setUp() {
        scheme = new Scheme();
        scheme.setId(1L);
        scheme.setName("Solar Scheme");

        beneficiary = new com.subsidytracker.common.entity.Beneficiary();
        beneficiary.setCategory(com.subsidytracker.common.enums.BeneficiaryCategory.GENERAL);

        application = new Application();
        application.setId(100L);
        application.setScheme(scheme);
        application.setBeneficiary(beneficiary);

        plan = new DisbursementPlan();
        plan.setId(5L);
        plan.setScheme(scheme);
        plan.setNumberOfStages(2);

        slab = new com.subsidytracker.common.entity.SchemeSlab();
        slab.setGrantAmount(new BigDecimal("100000"));
    }

    // ======================== generateSchedule ========================

    @Test
    void generateSchedule_success() {
        List<DisbursementStage> stages = twoStages();
        List<ApplicationDisbursementSchedule> expectedSchedules = buildExpectedSchedules(application, stages);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.of(plan));
        when(stageRepository.findByPlanIdOrderBySequenceNumberAsc(5L)).thenReturn(stages);
        when(scheduleRepository.existsByApplicationId(100L)).thenReturn(false);
        when(schemeSlabRepository.findBySchemeIdAndCategory(1L, com.subsidytracker.common.enums.BeneficiaryCategory.GENERAL)).thenReturn(Optional.of(slab));
        when(scheduleRepository.saveAll(any())).thenReturn(expectedSchedules);

        List<ApplicationDisbursementSchedule> result = scheduleGenerationService.generateSchedule(100L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> s.getStatus() == DisbursementScheduleStatus.PENDING);
        verify(scheduleRepository).saveAll(any());
    }

    @Test
    void generateSchedule_throwsWhenApplicationNotFound() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application");
    }

    @Test
    void generateSchedule_throwsWhenPlanNotFoundForScheme() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DisbursementPlan");
    }

    @Test
    void generateSchedule_throwsWhenPlanHasNoStages() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.of(plan));
        when(stageRepository.findByPlanIdOrderBySequenceNumberAsc(5L)).thenReturn(List.of());
        when(scheduleRepository.existsByApplicationId(100L)).thenReturn(false);

        assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("no configured stages");
    }

    @Test
    void generateSchedule_throwsWhenScheduleAlreadyExists() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.of(plan));
        when(stageRepository.findByPlanIdOrderBySequenceNumberAsc(5L)).thenReturn(twoStages());
        when(scheduleRepository.existsByApplicationId(100L)).thenReturn(true);

        assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already been generated");
    }

    @Test
    void generateSchedule_throwsWhenSchemeSlabNotFound() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.of(plan));
        when(stageRepository.findByPlanIdOrderBySequenceNumberAsc(5L)).thenReturn(twoStages());
        when(scheduleRepository.existsByApplicationId(100L)).thenReturn(false);
        when(schemeSlabRepository.findBySchemeIdAndCategory(1L, com.subsidytracker.common.enums.BeneficiaryCategory.GENERAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleGenerationService.generateSchedule(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("No SchemeSlab configured");
    }

    @Test
    void generateSchedule_eachEntryIsInitializedWithPendingStatus() {
        List<DisbursementStage> stages = twoStages();

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.of(plan));
        when(stageRepository.findByPlanIdOrderBySequenceNumberAsc(5L)).thenReturn(stages);
        when(scheduleRepository.existsByApplicationId(100L)).thenReturn(false);
        when(schemeSlabRepository.findBySchemeIdAndCategory(1L, com.subsidytracker.common.enums.BeneficiaryCategory.GENERAL)).thenReturn(Optional.of(slab));
        when(scheduleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ApplicationDisbursementSchedule> result = scheduleGenerationService.generateSchedule(100L);

        assertThat(result).hasSize(2);
        result.forEach(s -> assertThat(s.getStatus()).isEqualTo(DisbursementScheduleStatus.PENDING));
    }

    // ======================== getScheduleByApplication ========================

    @Test
    void getScheduleByApplication_returnsSchedulesOrderedByStageSequenceNumber() {
        List<DisbursementStage> stages = twoStages();
        List<ApplicationDisbursementSchedule> schedules = buildExpectedSchedules(application, stages);

        when(scheduleRepository.findByApplicationIdOrderByStageSequenceNumberAsc(100L))
                .thenReturn(schedules);

        List<ApplicationDisbursementSchedule> result =
                scheduleGenerationService.getScheduleByApplication(100L);

        assertThat(result).hasSize(2);
        verify(scheduleRepository).findByApplicationIdOrderByStageSequenceNumberAsc(100L);
    }

    @Test
    void getScheduleByApplication_returnsEmptyListWhenNoScheduleExists() {
        when(scheduleRepository.findByApplicationIdOrderByStageSequenceNumberAsc(200L))
                .thenReturn(List.of());

        List<ApplicationDisbursementSchedule> result =
                scheduleGenerationService.getScheduleByApplication(200L);

        assertThat(result).isEmpty();
    }

    // ======================== Helpers ========================

    private List<DisbursementStage> twoStages() {
        DisbursementStage s1 = new DisbursementStage();
        s1.setId(1L);
        s1.setPlan(plan);
        s1.setStageName("Initial Release");
        s1.setSequenceNumber(1);
        s1.setPercentageOfGrant(new BigDecimal("60"));
        s1.setTriggerMilestone(TriggerMilestone.APPLICATION_APPROVAL);

        DisbursementStage s2 = new DisbursementStage();
        s2.setId(2L);
        s2.setPlan(plan);
        s2.setStageName("Final Release");
        s2.setSequenceNumber(2);
        s2.setPercentageOfGrant(new BigDecimal("40"));
        s2.setTriggerMilestone(TriggerMilestone.PROJECT_CLOSURE);

        return List.of(s1, s2);
    }

    private List<ApplicationDisbursementSchedule> buildExpectedSchedules(Application app,
                                                                           List<DisbursementStage> stages) {
        return stages.stream().map(stage -> {
            ApplicationDisbursementSchedule s = new ApplicationDisbursementSchedule();
            s.setApplication(app);
            s.setStage(stage);
            s.setStatus(DisbursementScheduleStatus.PENDING);
            s.setScheduledAmount(BigDecimal.ZERO);
            return s;
        }).toList();
    }
}
