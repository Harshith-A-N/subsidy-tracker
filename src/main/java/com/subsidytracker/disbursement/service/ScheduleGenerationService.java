package com.subsidytracker.disbursement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.SchemeSlab;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
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

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.service.AuditLogService;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.scheme.repository.RegionalBudgetRepository;

@Service
public class ScheduleGenerationService {

    private static final int DAYS_BETWEEN_STAGES = 7;

    private final ApplicationRepository applicationRepository;
    private final DisbursementPlanRepository planRepository;
    private final ComplianceMilestoneService complianceMilestoneService;
    private final DisbursementStageRepository stageRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;
    private final SchemeSlabRepository schemeSlabRepository;
    private final DisbursementMilestoneRepository milestoneRepository;
    private final RegionalBudgetRepository regionalBudgetRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ScheduleGenerationService(ApplicationRepository applicationRepository,
                                     DisbursementPlanRepository planRepository,
                                     ComplianceMilestoneService complianceMilestoneService,
                                     DisbursementStageRepository stageRepository,
                                     ApplicationDisbursementScheduleRepository scheduleRepository,
                                     SchemeSlabRepository schemeSlabRepository,
                                     DisbursementMilestoneRepository milestoneRepository,
                                     RegionalBudgetRepository regionalBudgetRepository,
                                     UserRepository userRepository,
                                     AuditLogService auditLogService) {
        this.applicationRepository = applicationRepository;
        this.planRepository = planRepository;
        this.complianceMilestoneService = complianceMilestoneService;
        this.stageRepository = stageRepository;
        this.scheduleRepository = scheduleRepository;
        this.schemeSlabRepository = schemeSlabRepository;
        this.milestoneRepository = milestoneRepository;
        this.regionalBudgetRepository = regionalBudgetRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    // ---------- Public API ----------

    @Transactional
    public ApplicationDisbursementSchedule releaseStage(Long scheduleId, Long userId) {
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            if (user.getRole() != Role.FINANCE_APPROVER && user.getRole() != Role.ADMIN) {
                throw new InvalidOperationException("Only Finance Approvers and Administrators can release disbursement funds.");
            }
        }

        ApplicationDisbursementSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationDisbursementSchedule", scheduleId));

        if (schedule.getStatus() == DisbursementScheduleStatus.RELEASED) {
            throw new InvalidOperationException("This disbursement stage has already been released.");
        }

        Long applicationId = schedule.getApplication().getId();
        List<ApplicationDisbursementSchedule> allSchedules = scheduleRepository
                .findByApplicationIdOrderByStageSequenceNumberAsc(applicationId);

        int index = -1;
        for (int i = 0; i < allSchedules.size(); i++) {
            if (allSchedules.get(i).getId().equals(scheduleId)) {
                index = i;
                break;
            }
        }

        if (index > 0) {
            ApplicationDisbursementSchedule prevSchedule = allSchedules.get(index - 1);
            if (prevSchedule.getStatus() != DisbursementScheduleStatus.RELEASED) {
                throw new InvalidOperationException("Previous stage (" + prevSchedule.getStage().getStageName()
                        + ") must be released before this stage can be released.");
            }

            DisbursementMilestone prevMilestone = milestoneRepository
                    .findByApplicationIdOrderBySequenceOrderAsc(applicationId).stream()
                    .filter(m -> m.getStage().getId().equals(prevSchedule.getStage().getId()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOperationException("No milestone found for previous stage."));

            if (prevMilestone.getComplianceStatus() != ComplianceStatus.COMPLETED) {
                throw new InvalidOperationException("Previous stage utilization proof (" + prevSchedule.getStage().getStageName()
                        + ") must be verified by an officer before releasing the next stage.");
            }
        }

        schedule.setStatus(DisbursementScheduleStatus.RELEASED);
        ApplicationDisbursementSchedule saved = scheduleRepository.save(schedule);

        String region = schedule.getApplication().getBeneficiary().getRegion();
        Long schemeId = schedule.getApplication().getScheme().getId();

        if (region != null) {
            regionalBudgetRepository.findBySchemeId(schemeId).stream()
                    .filter(rb -> region.equalsIgnoreCase(rb.getRegionName()))
                    .findFirst()
                    .ifPresent(rb -> {
                        BigDecimal current = rb.getUtilizedBudget() != null ? rb.getUtilizedBudget() : BigDecimal.ZERO;
                        rb.setUtilizedBudget(current.add(schedule.getScheduledAmount()));
                        regionalBudgetRepository.save(rb);
                    });
        }

        try {
            auditLogService.logEvent(
                    "ApplicationDisbursementSchedule",
                    schedule.getId(),
                    "STAGE_RELEASED",
                    userId != null ? userRepository.findById(userId).orElse(null) : null,
                    "Released stage " + schedule.getStage().getStageName() + " (" + schedule.getScheduledAmount() + ") for application id: " + applicationId);
        } catch (Exception e) {
            // Audit log failure must not block primary operation
        }

        return saved;
    }

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

        try {
            auditLogService.logEvent(
                    "ApplicationDisbursementSchedule",
                    applicationId,
                    "SCHEDULE_GENERATED",
                    (User) null,
                    "Generated disbursement schedule with " + savedSchedules.size() + " stages for application id: " + applicationId);
        } catch (Exception e) {
            // Audit log failure must not prevent primary operation success
        }

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
        // Anchor all stages in this schedule to the same reference date so the
        // per-stage offsets below stay consistent with each other.
        LocalDate firstDueDate = LocalDate.now();
        return stages.stream()
                .map(stage -> buildScheduleEntry(application, stage, grantAmount, firstDueDate))
                .toList();
    }

    private ApplicationDisbursementSchedule buildScheduleEntry(Application application,
                                                                DisbursementStage stage,
                                                                BigDecimal grantAmount,
                                                                LocalDate firstDueDate) {
        ApplicationDisbursementSchedule schedule = new ApplicationDisbursementSchedule();
        schedule.setApplication(application);
        schedule.setStage(stage);
        schedule.setStatus(DisbursementScheduleStatus.PENDING);

        BigDecimal scheduledAmount = grantAmount
                .multiply(stage.getPercentageOfGrant())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        schedule.setScheduledAmount(scheduledAmount);

        // Previously hardcoded to LocalDate.now() for every stage (with a TODO
        // acknowledging it was a placeholder), while the real staggered dates
        // only existed on DisbursementMilestone. Now computed with the same
        // stage-offset formula ComplianceMilestoneService uses when it builds
        // milestones from these schedules, so /api/disbursement/schedules/**
        // shows the actual due date instead of "due today" for every stage.
        schedule.setDueDate(
                firstDueDate.plusDays((long) (stage.getSequenceNumber() - 1) * DAYS_BETWEEN_STAGES));

        return schedule;
    }
}