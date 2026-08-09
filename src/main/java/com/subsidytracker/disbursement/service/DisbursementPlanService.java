package com.subsidytracker.disbursement.service;

import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.DisbursementPlanRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DisbursementPlanService {

    private final DisbursementPlanRepository planRepository;
    private final DisbursementStageRepository stageRepository;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;

    public DisbursementPlanService(DisbursementPlanRepository planRepository,
                                   DisbursementStageRepository stageRepository,
                                   SchemeRepository schemeRepository,
                                   UserRepository userRepository) {
        this.planRepository = planRepository;
        this.stageRepository = stageRepository;
        this.schemeRepository = schemeRepository;
        this.userRepository = userRepository;
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
