package com.subsidytracker.disbursement.service;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.SchemeSlab;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementPlanRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleGenerationService {

    private final ApplicationRepository applicationRepository;
    private final DisbursementPlanRepository planRepository;
    private final ComplianceMilestoneService complianceMilestoneService;
    private final DisbursementStageRepository stageRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;
    private final SchemeSlabRepository schemeSlabRepository;

    public ScheduleGenerationService(ApplicationRepository applicationRepository,
                                     DisbursementPlanRepository planRepository, ComplianceMilestoneService complianceMilestoneService,
                                     DisbursementStageRepository stageRepository,
                                     ApplicationDisbursementScheduleRepository scheduleRepository,
                                     SchemeSlabRepository schemeSlabRepository) {
        this.applicationRepository = applicationRepository;
        this.planRepository = planRepository;
        this.complianceMilestoneService = complianceMilestoneService;
        this.stageRepository = stageRepository;
        this.scheduleRepository = scheduleRepository;
        this.schemeSlabRepository = schemeSlabRepository;
    }

    // ---------- Public API ----------

    @Transactional
    public List<ApplicationDisbursementSchedule> generateSchedule(Long applicationId) {
        Application application = loadApplication(applicationId);
        DisbursementPlan plan = loadPlanForApplication(application);
        List<DisbursementStage> stages = loadOrderedStages(plan);

        validateNoDuplicateSchedule(applicationId);
        validatePlanHasStages(stages, plan);

        BigDecimal applicableGrantAmount = resolveGrantAmount(application);

        List<ApplicationDisbursementSchedule> schedules =
                buildSchedules(application, stages, applicableGrantAmount);

        List<ApplicationDisbursementSchedule> savedSchedules =
                scheduleRepository.saveAll(schedules);

        complianceMilestoneService.createMilestones(applicationId);

        return savedSchedules;
    }

    public List<ApplicationDisbursementSchedule> getScheduleByApplication(Long applicationId) {
        return scheduleRepository.findByApplicationIdOrderByStageSequenceNumberAsc(applicationId);
    }

    // ---------- Load Helpers ----------

    private Application loadApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
    }

    private DisbursementPlan loadPlanForApplication(Application application) {
        Long schemeId = application.getScheme().getId();
        return planRepository.findBySchemeId(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("DisbursementPlan for Scheme", schemeId));
    }

    private List<DisbursementStage> loadOrderedStages(DisbursementPlan plan) {
        return stageRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
    }

    // ---------- Slab Resolution ----------

    private BigDecimal resolveGrantAmount(Application application) {
        Long schemeId = application.getScheme().getId();
        SchemeSlab slab = schemeSlabRepository.findBySchemeIdAndCategory(schemeId,
                        application.getBeneficiary().getCategory())
                .orElseThrow(() -> new InvalidOperationException(
                        "No SchemeSlab configured for scheme id: " + schemeId
                                + " and beneficiary category: " + application.getBeneficiary().getCategory()
                                + ". Cannot generate disbursement schedule."));
        return slab.getGrantAmount();
    }

    // ---------- Validation Helpers ----------

    private void validateNoDuplicateSchedule(Long applicationId) {
        if (scheduleRepository.existsByApplicationId(applicationId)) {
            throw new InvalidOperationException(
                    "A disbursement schedule has already been generated for application id: " + applicationId);
        }
    }

    private void validatePlanHasStages(List<DisbursementStage> stages, DisbursementPlan plan) {
        if (stages.isEmpty()) {
            throw new InvalidOperationException(
                    "Disbursement plan (id: " + plan.getId() + ") has no configured stages. Cannot generate schedule.");
        }
    }

    // ---------- Build Helpers ----------

    private List<ApplicationDisbursementSchedule> buildSchedules(Application application,
                                                                  List<DisbursementStage> stages,
                                                                  BigDecimal grantAmount) {
        return stages.stream()
                .map(stage -> buildScheduleEntry(application, stage, grantAmount))
                .toList();
    }

    private ApplicationDisbursementSchedule buildScheduleEntry(Application application,
                                                                DisbursementStage stage,
                                                                BigDecimal grantAmount) {
        ApplicationDisbursementSchedule schedule = new ApplicationDisbursementSchedule();
        schedule.setApplication(application);
        schedule.setStage(stage);
        schedule.setStatus(DisbursementScheduleStatus.PENDING);

        BigDecimal scheduledAmount = grantAmount
                .multiply(stage.getPercentageOfGrant())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        schedule.setScheduledAmount(scheduledAmount);

        // TODO: Calculate actual dueDate based on the approval date and stage trigger policy.
        //       Final logic depends on the disbursement policy to be defined in a later milestone.
        schedule.setDueDate(LocalDate.now());

        return schedule;
    }
}
