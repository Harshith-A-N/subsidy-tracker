package com.subsidytracker.disbursement.service;

import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.TriggerMilestone;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.DisbursementPlanRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementPlanServiceTest {

    @Mock private DisbursementPlanRepository planRepository;
    @Mock private DisbursementStageRepository stageRepository;
    @Mock private SchemeRepository schemeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private DisbursementPlanService planService;

    private Scheme scheme;
    private User user;

    @BeforeEach
    void setUp() {
        scheme = new Scheme();
        scheme.setId(1L);
        scheme.setName("Solar Scheme");

        user = new User();
        user.setId(10L);
    }

    // ======================== createPlan ========================

    @Test
    void createPlan_success() {
        List<DisbursementStage> stages = validTwoStages();

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        DisbursementPlan savedPlan = planWithScheme();
        when(planRepository.save(any())).thenReturn(savedPlan);
        when(stageRepository.saveAll(any())).thenReturn(stages);

        DisbursementPlan result = planService.createPlan(1L, 10L, stages);

        assertThat(result).isNotNull();
        assertThat(result.getScheme().getId()).isEqualTo(1L);
        verify(stageRepository).saveAll(stages);
    }

    @Test
    void createPlan_throwsWhenPlanAlreadyExistsForScheme() {
        List<DisbursementStage> stages = validTwoStages();
        DisbursementPlan existing = planWithScheme();

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createPlan_throwsWhenCreatedByUserIdIsNull() {
        List<DisbursementStage> stages = validTwoStages();

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, null, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("mandatory");
    }

    // ======================== validateStages – empty list ========================

    @Test
    void createPlan_throwsWhenStageListIsEmpty() {
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, List.of()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("At least one");
    }

    // ======================== validateStages – percentage rules ========================

    @Test
    void createPlan_throwsWhenPercentagesTotalIsNotHundred() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", 1, new BigDecimal("60"), TriggerMilestone.APPLICATION_APPROVAL),
                stage("Stage B", 2, new BigDecimal("30"), TriggerMilestone.GROUND_VERIFICATION)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("100%");
    }

    @Test
    void createPlan_throwsWhenPercentageIsZero() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", 1, BigDecimal.ZERO, TriggerMilestone.APPLICATION_APPROVAL)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void createPlan_throwsWhenPercentageIsNull() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", 1, null, TriggerMilestone.APPLICATION_APPROVAL)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void createPlan_throwsWhenPercentageExceedsHundred() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", 1, new BigDecimal("150"), TriggerMilestone.APPLICATION_APPROVAL)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("<= 100");
    }

    // ======================== validateStages – sequence number rules ========================

    @Test
    void createPlan_throwsWhenSequenceNumberIsNull() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", null, new BigDecimal("100"), TriggerMilestone.APPLICATION_APPROVAL)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void createPlan_throwsWhenSequenceNumberIsZeroOrNegative() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", 0, new BigDecimal("100"), TriggerMilestone.APPLICATION_APPROVAL)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void createPlan_throwsOnDuplicateSequenceNumbers() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", 1, new BigDecimal("50"), TriggerMilestone.APPLICATION_APPROVAL),
                stage("Stage B", 1, new BigDecimal("50"), TriggerMilestone.GROUND_VERIFICATION)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Duplicate sequence number");
    }

    // ======================== validateStages – stage name rules ========================

    @Test
    void createPlan_throwsOnDuplicateStageNames() {
        List<DisbursementStage> stages = List.of(
                stage("Release", 1, new BigDecimal("50"), TriggerMilestone.APPLICATION_APPROVAL),
                stage("Release", 2, new BigDecimal("50"), TriggerMilestone.GROUND_VERIFICATION)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Duplicate stage name");
    }

    // ======================== validateStages – trigger milestone ========================

    @Test
    void createPlan_throwsWhenTriggerMilestoneIsNull() {
        List<DisbursementStage> stages = List.of(
                stage("Stage A", 1, new BigDecimal("100"), null)
        );

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(scheme));
        when(planRepository.findBySchemeId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.createPlan(1L, 10L, stages))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Trigger milestone is required");
    }

    // ======================== getPlanBySchemeId ========================

    @Test
    void getPlanBySchemeId_throwsWhenPlanNotFound() {
        when(planRepository.findBySchemeId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.getPlanBySchemeId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ======================== deletePlan ========================

    @Test
    void deletePlan_throwsWhenPlanNotFound() {
        when(planRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.deletePlan(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePlan_deletesStagesAndPlan() {
        DisbursementPlan plan = planWithScheme();
        plan.setId(5L);
        List<DisbursementStage> stages = validTwoStages();

        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(stageRepository.findByPlanIdOrderBySequenceNumberAsc(5L)).thenReturn(stages);

        planService.deletePlan(5L);

        verify(stageRepository).deleteAll(stages);
        verify(planRepository).delete(plan);
    }

    // ======================== Helpers ========================

    private List<DisbursementStage> validTwoStages() {
        return List.of(
                stage("Initial Release", 1, new BigDecimal("60"), TriggerMilestone.APPLICATION_APPROVAL),
                stage("Final Release", 2, new BigDecimal("40"), TriggerMilestone.PROJECT_CLOSURE)
        );
    }

    private DisbursementStage stage(String name, Integer seq, BigDecimal pct, TriggerMilestone milestone) {
        DisbursementStage s = new DisbursementStage();
        s.setStageName(name);
        s.setSequenceNumber(seq);
        s.setPercentageOfGrant(pct);
        s.setTriggerMilestone(milestone);
        return s;
    }

    private DisbursementPlan planWithScheme() {
        DisbursementPlan plan = new DisbursementPlan();
        plan.setScheme(scheme);
        plan.setCreatedBy(user);
        plan.setNumberOfStages(2);
        return plan;
    }
}
