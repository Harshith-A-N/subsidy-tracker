package com.subsidytracker.disbursement.service;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplianceMilestoneService {

    private static final int DAYS_BETWEEN_MILESTONES = 7;

    private final DisbursementMilestoneRepository milestoneRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;
    private final ApplicationRepository applicationRepository;

    public ComplianceMilestoneService(
            DisbursementMilestoneRepository milestoneRepository,
            ApplicationDisbursementScheduleRepository scheduleRepository,
            ApplicationRepository applicationRepository) {

        this.milestoneRepository = milestoneRepository;
        this.scheduleRepository = scheduleRepository;
        this.applicationRepository = applicationRepository;
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

        LocalDate firstDueDate = LocalDate.now();

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

                    milestone.setDueDate(
                            firstDueDate.plusDays(
                                    (long) (stage.getSequenceNumber() - 1)
                                            * DAYS_BETWEEN_MILESTONES));

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
     * Mark a compliance milestone as completed.
     * Completing it releases the corresponding disbursement stage.
     */
    @Transactional
    public DisbursementMilestone completeMilestone(Long milestoneId) {

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

        if (milestone.getComplianceStatus()
                == ComplianceStatus.OVERDUE) {

            throw new InvalidOperationException(
                    "Overdue milestone cannot be completed. "
                            + "Milestone ID: " + milestoneId);
        }

        milestone.setComplianceStatus(
                ComplianceStatus.COMPLETED);

        milestone.setCompletedAt(
                LocalDateTime.now());

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
                });

        return milestoneRepository.save(milestone);
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
}