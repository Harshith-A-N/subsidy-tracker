package com.subsidytracker.disbursement.controller;

import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.service.ComplianceMilestoneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disbursement/compliance")
public class ComplianceMilestoneController {

    private final ComplianceMilestoneService complianceMilestoneService;

    public ComplianceMilestoneController(
            ComplianceMilestoneService complianceMilestoneService) {
        this.complianceMilestoneService = complianceMilestoneService;
    }

    @PostMapping("/application/{applicationId}")
    public ResponseEntity<List<DisbursementMilestone>> createMilestones(
            @PathVariable Long applicationId) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(complianceMilestoneService.createMilestones(applicationId));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<DisbursementMilestone>> getApplicationMilestones(
            @PathVariable Long applicationId) {

        return ResponseEntity.ok(
                complianceMilestoneService
                        .getApplicationMilestones(applicationId));
    }

    @PutMapping("/{milestoneId}/complete")
    public ResponseEntity<DisbursementMilestone> completeMilestone(
            @PathVariable Long milestoneId) {

        return ResponseEntity.ok(
                complianceMilestoneService.completeMilestone(milestoneId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<DisbursementMilestone>> getPendingMilestones() {

        return ResponseEntity.ok(
                complianceMilestoneService.getPendingMilestones());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<DisbursementMilestone>> getOverdueMilestones() {

        return ResponseEntity.ok(
                complianceMilestoneService.getOverdueMilestones());
    }
}