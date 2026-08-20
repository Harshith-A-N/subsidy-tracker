package com.subsidytracker.disbursement.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementPlanRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;

@Service
public class DisbursementPlanService {

    private final DisbursementPlanRepository planRepository;
    private final DisbursementStageRepository stageRepository;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;

    public DisbursementPlanService(DisbursementPlanRepository planRepository,
                                   DisbursementStageRepository stageRepository,
                                   SchemeRepository schemeRepository,
                                   UserRepository userRepository,
                                   ApplicationDisbursementScheduleRepository scheduleRepository) {
        this.planRepository = planRepository;
        this.stageRepository = stageRepository;
        this.schemeRepository = schemeRepository;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
    }

    // ---------- Create ----------

    @Transactional
    public DisbursementPlan createPlan(Long schemeId, Long createdByUserId, List<DisbursementStage> stages) {
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", schemeId));

        planRepository.findBySchemeId(schemeId).ifPresent(existing -> {
            throw new InvalidOperationException("A disbursement plan already exists for scheme: " + scheme.getName());
        });

        validateStages(stages);

        if (createdByUserId == null) {
            throw new InvalidOperationException("Created by user id is mandatory when creating a disbursement plan.");
        }

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", createdByUserId));

        DisbursementPlan plan = new DisbursementPlan();
        plan.setScheme(scheme);
        plan.setNumberOfStages(stages.size());
        plan.setCreatedBy(creator);

        DisbursementPlan savedPlan = planRepository.save(plan);

        stages.forEach(stage -> stage.setPlan(savedPlan));
        stageRepository.saveAll(stages);

        return savedPlan;
    }

    // ---------- Update ----------

    @Transactional
    public DisbursementPlan updatePlan(Long planId, List<DisbursementStage> stages) {
        DisbursementPlan plan = findPlanOrThrow(planId);

        validateStages(stages);

        // Remove existing stages and replace with new configuration
        List<DisbursementStage> existingStages = stageRepository.findByPlanIdOrderBySequenceNumberAsc(planId);
        validateNoLiveSchedules(planId, existingStages,
                "Cannot update disbursement plan (id: " + planId + "): one or more applications already "
                        + "have a generated schedule against its current stages. Generate/edit plans "
                        + "before any application schedule exists, or introduce plan versioning if "
                        + "in-flight edits are required.");
        stageRepository.deleteAll(existingStages);

        plan.setNumberOfStages(stages.size());
        DisbursementPlan savedPlan = planRepository.save(plan);

        stages.forEach(stage -> {
            stage.setId(null); // ensure new entities are created
            stage.setPlan(savedPlan);
        });
        stageRepository.saveAll(stages);

        return savedPlan;
    }

    // ---------- Delete ----------

    @Transactional
    public void deletePlan(Long planId) {
        DisbursementPlan plan = findPlanOrThrow(planId);

        List<DisbursementStage> stages = stageRepository.findByPlanIdOrderBySequenceNumberAsc(planId);
        validateNoLiveSchedules(planId, stages,
                "Cannot delete disbursement plan (id: " + planId + "): one or more applications already "
                        + "have a generated schedule against its stages. Deleting it would orphan those "
                        + "schedules and their compliance milestones.");
        stageRepository.deleteAll(stages);

        planRepository.delete(plan);
    }

    // ---------- Get by Scheme ----------

    public DisbursementPlan getPlanBySchemeId(Long schemeId) {
        return planRepository.findBySchemeId(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("DisbursementPlan for Scheme", schemeId));
    }

    // ---------- Get by Id ----------

    public DisbursementPlan getPlanById(Long planId) {
        return findPlanOrThrow(planId);
    }

    // ---------- Helpers ----------

    private DisbursementPlan findPlanOrThrow(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("DisbursementPlan", planId));
    }

    /**
     * DisbursementStage rows are referenced by non-nullable FKs from both
     * ApplicationDisbursementSchedule.stage_id and DisbursementMilestone.stage_id.
     * Deleting a stage that either table still points to would either throw a raw
     * DB foreign-key violation or, if the DB allows it, silently orphan those rows.
     * Block the edit/delete up front with a clear, actionable message instead.
     */
    private void validateNoLiveSchedules(Long planId, List<DisbursementStage> stages, String message) {
        if (stages.isEmpty()) {
            return;
        }
        List<Long> stageIds = stages.stream().map(DisbursementStage::getId).toList();
        if (scheduleRepository.existsByStageIdIn(stageIds)) {
            throw new InvalidOperationException(message);
        }
    }

    private void validateStages(List<DisbursementStage> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new InvalidOperationException("At least one disbursement stage is required.");
        }

        // Validate individual stage fields
        for (DisbursementStage stage : stages) {
            if (stage.getPercentageOfGrant() == null) {
                throw new InvalidOperationException("Percentage of grant cannot be null.");
            }
            if (stage.getPercentageOfGrant().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidOperationException(
                        "Percentage of grant must be greater than 0. Found: " + stage.getPercentageOfGrant() + "%.");
            }
            if (stage.getPercentageOfGrant().compareTo(new BigDecimal("100")) > 0) {
                throw new InvalidOperationException(
                        "Percentage of grant must be <= 100. Found: " + stage.getPercentageOfGrant() + "%.");
            }
            if (stage.getSequenceNumber() == null) {
                throw new InvalidOperationException("Sequence number cannot be null.");
            }
            if (stage.getSequenceNumber() <= 0) {
                throw new InvalidOperationException(
                        "Sequence number must be greater than 0. Found: " + stage.getSequenceNumber() + ".");
            }
            if (stage.getTriggerMilestone() == null) {
                throw new InvalidOperationException(
                        "Trigger milestone is required for each stage. Stage '" + stage.getStageName() + "' has no trigger milestone.");
            }
            if (stage.getDueDateOffsetDays() == null) {
                throw new InvalidOperationException(
                        "Due date offset days cannot be null. Stage '" + stage.getStageName() + "' has no configured offset.");
            }
            if (stage.getDueDateOffsetDays() < 0) {
                throw new InvalidOperationException(
                        "Due date offset days must be non-negative. Found: " + stage.getDueDateOffsetDays() + " for stage '" + stage.getStageName() + "'.");
            }
        }

        // Validate stage percentages total exactly 100%
        BigDecimal totalPercentage = stages.stream()
                .map(DisbursementStage::getPercentageOfGrant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new InvalidOperationException(
                    "Stage percentages must total exactly 100%. Current total: " + totalPercentage + "%.");
        }

        // Validate unique sequence numbers
        Set<Integer> sequenceNumbers = new HashSet<>();
        for (DisbursementStage stage : stages) {
            if (!sequenceNumbers.add(stage.getSequenceNumber())) {
                throw new InvalidOperationException(
                        "Duplicate sequence number: " + stage.getSequenceNumber() + ". Sequence numbers must be unique.");
            }
        }

        // Validate unique stage names
        Set<String> stageNames = new HashSet<>();
        for (DisbursementStage stage : stages) {
            if (stage.getStageName() == null || stage.getStageName().isBlank()) {
                throw new InvalidOperationException("Stage name cannot be empty.");
            }
            if (!stageNames.add(stage.getStageName().trim().toLowerCase())) {
                throw new InvalidOperationException(
                        "Duplicate stage name: '" + stage.getStageName() + "'. Stage names must be unique.");
            }
        }
    }
}