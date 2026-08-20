package com.subsidytracker.disbursement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.DisbursementScheduleStatus;
import com.subsidytracker.common.enums.DisbursementStatus;
import com.subsidytracker.common.enums.MilestoneType;
import com.subsidytracker.common.enums.TriggerMilestone;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.scheme.repository.RegionalBudgetRepository;
import com.subsidytracker.common.service.AuditLogService;

@Service
public class ComplianceMilestoneService {

    private final DisbursementMilestoneRepository milestoneRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;
    private final ApplicationRepository applicationRepository;
    private final RegionalBudgetRepository regionalBudgetRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ComplianceMilestoneService(
            DisbursementMilestoneRepository milestoneRepository,
            ApplicationDisbursementScheduleRepository scheduleRepository,
            ApplicationRepository applicationRepository,
            RegionalBudgetRepository regionalBudgetRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.milestoneRepository = milestoneRepository;
        this.scheduleRepository = scheduleRepository;
        this.applicationRepository = applicationRepository;
        this.regionalBudgetRepository = regionalBudgetRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Creates compliance milestones from the application's disbursement schedule.
     */
    @Transactional
    public List<DisbursementMilestone> createMilestones(Long applicationId) {

        applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Application", applicationId));

        List<ApplicationDisbursementSchedule> schedules =
                scheduleRepository.findByApplicationIdOrderByStageSequenceNumberAsc(
                        applicationId);

        if (schedules.isEmpty()) {
            throw new InvalidOperationException(
                    "No disbursement schedule exists for application "
                            + applicationId);
        }

        List<DisbursementMilestone> existing =
                milestoneRepository.findByApplicationIdOrderBySequenceOrderAsc(
                        applicationId);

        if (!existing.isEmpty()) {
            return existing;
        }

        List<DisbursementMilestone> milestones = schedules.stream()
                .map(schedule -> {

                    DisbursementStage stage = schedule.getStage();

                    DisbursementMilestone milestone =
                            new DisbursementMilestone();

                    milestone.setApplication(schedule.getApplication());
                    milestone.setStage(stage);

                    milestone.setMilestoneType(
                            convertMilestoneType(
                                    stage.getTriggerMilestone()));

                    milestone.setSequenceOrder(
                            stage.getSequenceNumber());

                    milestone.setDescription(
                            "Complete compliance requirement for stage: "
                                    + stage.getStageName());

                    milestone.setScheduledAmount(
                            schedule.getScheduledAmount());

                    // Previously recomputed the staggered due date here
                    // independently of ScheduleGenerationService (which used
                    // to just hardcode LocalDate.now() on the schedule side).
                    // Now both sides derive from the same
                    // ApplicationDisbursementSchedule.dueDate, so the schedule
                    // API and the milestone tracker can never show different
                    // dates for the same stage again.
                    milestone.setDueDate(schedule.getDueDate());

                    milestone.setComplianceStatus(
                            ComplianceStatus.PENDING);

                    milestone.setDisbursementStatus(
                            DisbursementStatus.NOT_RELEASED);

                    return milestone;
                })
                .toList();

        return milestoneRepository.saveAll(milestones);
    }

    /**
     * Mark a compliance milestone as completed using authenticated security context.
     * Completing it releases the corresponding disbursement stage.
     */
    @Transactional
    public DisbursementMilestone completeMilestone(Long milestoneId) {
        Long currentUserId = getCurrentUserIdFromSecurityContext();
        return completeMilestone(milestoneId, currentUserId);
    }

    /**
     * Mark a compliance milestone as completed by a specific user.
     * Completing it releases the corresponding disbursement stage.
     */
    @Transactional
    public DisbursementMilestone completeMilestone(Long milestoneId, Long completedByUserId) {

        DisbursementMilestone milestone =
                milestoneRepository.findById(milestoneId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "DisbursementMilestone",
                                        milestoneId));

        if (milestone.getComplianceStatus()
                == ComplianceStatus.COMPLETED) {
            return milestone;
        }

        /*
         * Overdue is a PAUSE, not a permanent block — per the project guide's
         * Module 3 flow: "Missed Due Date? -> Non-Compliance Flag -> Reminder
         * Sent -> Release Paused". A beneficiary who was late but eventually
         * satisfies the milestone should still be able to have it completed
         * and released; there was previously no path out of OVERDUE at all.
         */

        milestone.setComplianceStatus(
                ComplianceStatus.COMPLETED);

        milestone.setCompletedAt(
                LocalDateTime.now());

        if (completedByUserId != null) {
            User completedBy = userRepository.findById(completedByUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", completedByUserId));
            milestone.setCompletedBy(completedBy);
        }

        /*
         * Release the corresponding disbursement stage.
         * This is the simulated fund release required by Milestone 3.
         */
        List<ApplicationDisbursementSchedule> schedules =
                scheduleRepository.findByApplicationId(
                        milestone.getApplication().getId());

        schedules.stream()
                .filter(schedule ->
                        schedule.getStage().getId()
                                .equals(milestone.getStage().getId()))
                .findFirst()
                .ifPresent(schedule -> {
                    schedule.setStatus(
                            DisbursementScheduleStatus.RELEASED);
                    scheduleRepository.save(schedule);

                    /*
                     * RegionalBudget.utilizedBudget was always initialized to
                     * zero and never updated anywhere afterward — meaning any
                     * screen reading it directly (not via the analytics
                     * region-utilization query, which computes this figure
                     * live from released schedules) would show 0 forever,
                     * regardless of real disbursement activity. Fix that here,
                     * at the one place a release actually happens.
                     */
                    String region = milestone.getApplication()
                            .getBeneficiary().getRegion();
                    Long schemeId = milestone.getApplication()
                            .getScheme().getId();

                    if (region != null) {
                        regionalBudgetRepository.findBySchemeId(schemeId)
                                .stream()
                                .filter(rb -> region.equalsIgnoreCase(rb.getRegionName()))
                                .findFirst()
                                .ifPresent(rb -> {
                                    BigDecimal current = rb.getUtilizedBudget() != null
                                            ? rb.getUtilizedBudget() : BigDecimal.ZERO;
                                    rb.setUtilizedBudget(current.add(schedule.getScheduledAmount()));
                                    regionalBudgetRepository.save(rb);
                                });
                        // No matching RegionalBudget row is a configuration
                        // gap (no budget was ever allocated for this
                        // scheme+region), not a reason to block the release.
                    }
                });

        // Reuses the `schedules` list above: the entry we just released was
        // mutated in place (same managed entity), so this reflects the
        // post-release state without a second query.
        advanceApplicationStatusIfFullyDisbursed(milestone, schedules);

        DisbursementMilestone saved = milestoneRepository.save(milestone);

        try {
            auditLogService.logEvent(
                    "DisbursementMilestone",
                    milestone.getId(),
                    "MILESTONE_COMPLETED",
                    milestone.getCompletedBy(),
                    "Completed milestone " + milestone.getMilestoneType() + " for stage " + milestone.getStage().getStageName());
        } catch (Exception e) {
            // Audit log failure must not prevent primary operation success
        }

        return saved;
    }

    /**
     * Application.status previously never advanced past READY_FOR_DISBURSEMENT
     * — DISBURSED and COMPLETED were defined in the enum but never set
     * anywhere, so there was no way to distinguish "fully paid out"
     * applications from ones that were merely approved.
     *
     * Once every disbursement stage for the application has been RELEASED:
     * - if the milestone just completed was the final "Project/Scheme
     *   Closure" stage (per the guide's Module 3 flow diagram), the
     *   application is fully finished -> COMPLETED.
     * - otherwise, all funds are out but closure hasn't happened yet ->
     *   DISBURSED.
     */
    private void advanceApplicationStatusIfFullyDisbursed(
            DisbursementMilestone milestone,
            List<ApplicationDisbursementSchedule> schedules) {

        Application application = milestone.getApplication();

        if (application.getStatus() == ApplicationStatus.COMPLETED) {
            return;
        }

        boolean allReleased = !schedules.isEmpty() && schedules.stream()
                .allMatch(s -> s.getStatus() == DisbursementScheduleStatus.RELEASED);

        if (!allReleased) {
            return;
        }

        boolean isProjectClosure = milestone.getStage().getTriggerMilestone()
                == TriggerMilestone.PROJECT_CLOSURE;

        application.setStatus(
                isProjectClosure ? ApplicationStatus.COMPLETED : ApplicationStatus.DISBURSED);

        applicationRepository.save(application);
    }

    /**
     * Returns pending milestones.
     */
    public List<DisbursementMilestone> getPendingMilestones() {
        return milestoneRepository.findByComplianceStatus(
                ComplianceStatus.PENDING);
    }

    /**
     * Returns overdue milestones.
     */
    public List<DisbursementMilestone> getOverdueMilestones() {
        return milestoneRepository.findByComplianceStatus(
                ComplianceStatus.OVERDUE);
    }

    /**
     * Returns milestones belonging to an application.
     */
    public List<DisbursementMilestone> getApplicationMilestones(
            Long applicationId) {

        return milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(applicationId);
    }

    /**
     * Daily overdue checking.
     *
     * Runs every day at 1:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void flagOverdueMilestones() {

        List<DisbursementMilestone> overdueCandidates =
                milestoneRepository
                        .findByComplianceStatusAndDueDateBefore(
                                ComplianceStatus.PENDING,
                                LocalDate.now());

        for (DisbursementMilestone milestone : overdueCandidates) {

            milestone.setComplianceStatus(
                    ComplianceStatus.OVERDUE);

            milestoneRepository.save(milestone);

            try {
                auditLogService.logSystemEvent(
                        "DisbursementMilestone",
                        milestone.getId(),
                        "MILESTONE_OVERDUE",
                        "Milestone " + milestone.getId() + " for application "
                                + milestone.getApplication().getId() + " is overdue.");
            } catch (Exception e) {
                // Audit log failure must not prevent primary operation success
            }

            System.out.println(
                    "COMPLIANCE REMINDER: Milestone "
                            + milestone.getId()
                            + " for application "
                            + milestone.getApplication().getId()
                            + " is overdue.");
        }
    }

    private MilestoneType convertMilestoneType(TriggerMilestone trigger) {

        if (trigger == TriggerMilestone.APPLICATION_APPROVAL) {
            return MilestoneType.DOCUMENTATION;
        }

        if (trigger == TriggerMilestone.GROUND_VERIFICATION) {
            return MilestoneType.GROUND_VERIFICATION;
        }

        if (trigger == TriggerMilestone.UTILIZATION_PROOF) {
            return MilestoneType.UTILIZATION_PROOF;
        }

        if (trigger == TriggerMilestone.PROJECT_CLOSURE) {
            return MilestoneType.UTILIZATION_PROOF;
        }

        throw new IllegalArgumentException(
                "Unsupported trigger milestone: " + trigger);
    }

    private Long getCurrentUserIdFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email).map(User::getId).orElse(null);
        }
        return null;
    }
}