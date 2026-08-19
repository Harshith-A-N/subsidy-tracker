package com.subsidytracker.disbursement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.dto.DisbursementPlanRequest;
import com.subsidytracker.disbursement.dto.DisbursementPlanResponse;
import com.subsidytracker.disbursement.dto.DisbursementStageRequest;
import com.subsidytracker.disbursement.dto.DisbursementStageResponse;
import com.subsidytracker.disbursement.dto.ScheduleEntryResponse;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.disbursement.service.DisbursementPlanService;
import com.subsidytracker.disbursement.service.ScheduleGenerationService;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;

@RestController
@RequestMapping("/api/disbursement")
public class DisbursementController {

    private final DisbursementPlanService planService;
    private final ScheduleGenerationService scheduleGenerationService;
    private final DisbursementStageRepository stageRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public DisbursementController(DisbursementPlanService planService,
                                  ScheduleGenerationService scheduleGenerationService,
                                  DisbursementStageRepository stageRepository,
                                  UserRepository userRepository,
                                  ApplicationRepository applicationRepository) {
        this.planService = planService;
        this.scheduleGenerationService = scheduleGenerationService;
        this.stageRepository = stageRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    // ---------- Plan Management ----------

    @PostMapping("/plans")
    public ResponseEntity<DisbursementPlanResponse> createPlan(@RequestBody DisbursementPlanRequest request,
                                                                Authentication authentication) {
        long userId = resolveUserId(authentication);
        List<DisbursementStage> stages = toStageEntities(request.getStages());
        DisbursementPlan plan = planService.createPlan(request.getSchemeId(), userId, stages);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPlanResponse(plan));
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<DisbursementPlanResponse> updatePlan(@PathVariable Long planId,
                                                                @RequestBody List<DisbursementStageRequest> stageRequests) {
        List<DisbursementStage> stages = toStageEntities(stageRequests);
        DisbursementPlan plan = planService.updatePlan(planId, stages);
        return ResponseEntity.ok(toPlanResponse(plan));
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long planId) {
        planService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<DisbursementPlanResponse> getPlanById(@PathVariable Long planId) {
        return ResponseEntity.ok(toPlanResponse(planService.getPlanById(planId)));
    }

    @GetMapping("/plans/scheme/{schemeId}")
    public ResponseEntity<DisbursementPlanResponse> getPlanByScheme(@PathVariable Long schemeId) {
        return ResponseEntity.ok(toPlanResponse(planService.getPlanBySchemeId(schemeId)));
    }

    // ---------- Schedule Generation ----------

    @PostMapping("/schedules/generate/{applicationId}")
    public ResponseEntity<List<ScheduleEntryResponse>> generateSchedule(@PathVariable Long applicationId) {
        List<ApplicationDisbursementSchedule> schedules = scheduleGenerationService.generateSchedule(applicationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toScheduleResponseList(schedules));
    }

    // ---------- Schedule Retrieval ----------

    /**
     * Beneficiaries may only view the schedule for their own application;
     * officers/finance/admin may view any application's schedule.
     */
    @GetMapping("/schedules/application/{applicationId}")
    public ResponseEntity<List<ScheduleEntryResponse>> getSchedule(@PathVariable Long applicationId,
                                                                    Authentication authentication) {
        assertCanViewApplication(applicationId, authentication);
        List<ApplicationDisbursementSchedule> schedules =
                scheduleGenerationService.getScheduleByApplication(applicationId);
        return ResponseEntity.ok(toScheduleResponseList(schedules));
    }

    // ---------- Mapping Helpers ----------

    private List<DisbursementStage> toStageEntities(List<DisbursementStageRequest> requests) {
        return requests.stream()
                .map(r -> {
                    DisbursementStage stage = new DisbursementStage();
                    stage.setStageName(r.getStageName());
                    stage.setSequenceNumber(r.getSequenceNumber());
                    stage.setPercentageOfGrant(r.getPercentageOfGrant());
                    stage.setTriggerMilestone(r.getTriggerMilestone());
                    return stage;
                })
                .toList();
    }

    private DisbursementPlanResponse toPlanResponse(DisbursementPlan plan) {
        DisbursementPlanResponse response = new DisbursementPlanResponse();
        response.setId(plan.getId());
        response.setSchemeId(plan.getScheme().getId());
        response.setNumberOfStages(plan.getNumberOfStages());
        response.setCreatedAt(plan.getCreatedAt());
        if (plan.getCreatedBy() != null) {
            response.setCreatedById(plan.getCreatedBy().getId());
        }
        List<DisbursementStage> stages = stageRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
        response.setStages(stages.stream().map(this::toStageResponse).toList());
        return response;
    }

    private DisbursementStageResponse toStageResponse(DisbursementStage stage) {
        DisbursementStageResponse response = new DisbursementStageResponse();
        response.setId(stage.getId());
        response.setPlanId(stage.getPlan().getId());
        response.setStageName(stage.getStageName());
        response.setSequenceNumber(stage.getSequenceNumber());
        response.setPercentageOfGrant(stage.getPercentageOfGrant());
        response.setTriggerMilestone(stage.getTriggerMilestone());
        return response;
    }

    private List<ScheduleEntryResponse> toScheduleResponseList(List<ApplicationDisbursementSchedule> schedules) {
        return schedules.stream().map(this::toScheduleResponse).toList();
    }

    private ScheduleEntryResponse toScheduleResponse(ApplicationDisbursementSchedule schedule) {
        ScheduleEntryResponse response = new ScheduleEntryResponse();
        response.setId(schedule.getId());
        response.setApplicationId(schedule.getApplication().getId());
        response.setStageId(schedule.getStage().getId());
        response.setStageName(schedule.getStage().getStageName());
        response.setStageSequenceNumber(schedule.getStage().getSequenceNumber());
        response.setScheduledAmount(schedule.getScheduledAmount());
        response.setDueDate(schedule.getDueDate());
        response.setStatus(schedule.getStatus());
        return response;
    }

    // ---------- Auth Helpers ----------

    private long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));
        return user.getId();
    }

    /**
     * Mirrors ApplicationService.getApplicationById's ownership check:
     * a BENEFICIARY may only see data for applications tied to their own
     * beneficiary profile; every other role passes through unrestricted.
     */
    private void assertCanViewApplication(Long applicationId, Authentication authentication) {
        long currentUserId = resolveUserId(authentication);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (currentUser.getRole() == Role.BENEFICIARY) {
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
            Beneficiary beneficiary = application.getBeneficiary();
            if (beneficiary.getUser() == null || beneficiary.getUser().getId() != currentUserId) {
                throw new InvalidOperationException("You are not authorized to view this application's schedule.");
            }
        }
    }
}