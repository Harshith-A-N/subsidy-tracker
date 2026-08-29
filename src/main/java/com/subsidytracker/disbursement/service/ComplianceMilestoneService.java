package com.subsidytracker.disbursement.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.enums.TriggerMilestone;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.common.service.AuditLogService;

@Service
public class ComplianceMilestoneService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceMilestoneService.class);

    private final DisbursementMilestoneRepository milestoneRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AuditLogService auditLogService;

    public ComplianceMilestoneService(
            DisbursementMilestoneRepository milestoneRepository,
            ApplicationDisbursementScheduleRepository scheduleRepository,
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            AuditLogService auditLogService) {

        this.milestoneRepository = milestoneRepository;
        this.scheduleRepository = scheduleRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
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
     */
    @Transactional
    public DisbursementMilestone completeMilestone(Long milestoneId) {
        Long currentUserId = getCurrentUserIdFromSecurityContext();
        return completeMilestone(milestoneId, currentUserId);
    }

    /**
     * Mark a compliance milestone as completed by an authorized officer.
     */
    @Transactional
    public DisbursementMilestone completeMilestone(Long milestoneId, Long completedByUserId) {

        DisbursementMilestone milestone =
                milestoneRepository.findById(milestoneId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "DisbursementMilestone",
                                        milestoneId));

        if (milestone.getComplianceStatus() == ComplianceStatus.COMPLETED) {
            return milestone;
        }

        User completedBy = null;
        if (completedByUserId != null) {
            completedBy = userRepository.findById(completedByUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", completedByUserId));

            if (completedBy.getRole() == Role.FINANCE_APPROVER) {
                throw new InvalidOperationException("Finance Approvers are not permitted to complete compliance milestones. Officers must verify compliance proof.");
            }

            if (completedBy.getRole() == Role.FIELD_OFFICER || completedBy.getRole() == Role.DISTRICT_OFFICER) {
                String benRegion = milestone.getApplication().getBeneficiary().getRegion();
                String offRegion = completedBy.getRegion();
                if (benRegion != null && offRegion != null && !benRegion.equalsIgnoreCase(offRegion)) {
                    throw new InvalidOperationException("This application is not in your assigned region.");
                }
            }
        }

        List<ApplicationDisbursementSchedule> schedules =
                scheduleRepository.findByApplicationId(
                        milestone.getApplication().getId());

        ApplicationDisbursementSchedule schedule = schedules.stream()
                .filter(s -> s.getStage().getId().equals(milestone.getStage().getId()))
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("No schedule entry found for stage."));

        if (schedule.getStatus() != DisbursementScheduleStatus.RELEASED) {
            throw new InvalidOperationException("Funds for stage '" + milestone.getStage().getStageName()
                    + "' must be released before compliance can be completed.");
        }

        boolean hasProof = documentRepository.existsByApplicationIdAndStageId(
                milestone.getApplication().getId(), milestone.getStage().getId());
        if (!hasProof) {
            throw new InvalidOperationException("No utilization proof has been uploaded for stage '"
                    + milestone.getStage().getStageName() + "'. Officer verification requires proof.");
        }

        milestone.setComplianceStatus(ComplianceStatus.COMPLETED);
        milestone.setCompletedAt(LocalDateTime.now());
        if (completedBy != null) {
            milestone.setCompletedBy(completedBy);
        }

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
            logger.warn("Failed to log audit event [entityType=DisbursementMilestone, entityId={}, action=MILESTONE_COMPLETED]: {}",
                    milestone.getId(), e.getMessage(), e);
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

    public Page<DisbursementMilestone> getPendingMilestones(Pageable pageable) {
        return milestoneRepository.findByComplianceStatus(
                ComplianceStatus.PENDING, pageable);
    }

    /**
     * Returns overdue milestones.
     */
    public List<DisbursementMilestone> getOverdueMilestones() {
        return milestoneRepository.findByComplianceStatus(
                ComplianceStatus.OVERDUE);
    }

    public Page<DisbursementMilestone> getOverdueMilestones(Pageable pageable) {
        return milestoneRepository.findByComplianceStatus(
                ComplianceStatus.OVERDUE, pageable);
    }

    /**
     * Returns milestones belonging to an application.
     */
    public List<DisbursementMilestone> getApplicationMilestones(
            Long applicationId) {

        return milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(applicationId);
    }

    public Page<DisbursementMilestone> getApplicationMilestones(
            Long applicationId, Pageable pageable) {

        return milestoneRepository
                .findByApplicationIdOrderBySequenceOrderAsc(applicationId, pageable);
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
                logger.warn("Failed to log audit event [entityType=DisbursementMilestone, entityId={}, action=MILESTONE_OVERDUE]: {}",
                        milestone.getId(), e.getMessage(), e);
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