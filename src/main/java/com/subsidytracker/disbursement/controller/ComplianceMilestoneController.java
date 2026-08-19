package com.subsidytracker.disbursement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.entity.Beneficiary;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.Role;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.service.ComplianceMilestoneService;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.UserRepository;

@RestController
@RequestMapping("/api/disbursement/compliance")
public class ComplianceMilestoneController {

    private final ComplianceMilestoneService complianceMilestoneService;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public ComplianceMilestoneController(
            ComplianceMilestoneService complianceMilestoneService,
            ApplicationRepository applicationRepository,
            UserRepository userRepository) {
        this.complianceMilestoneService = complianceMilestoneService;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/application/{applicationId}")
    public ResponseEntity<List<DisbursementMilestone>> createMilestones(
            @PathVariable Long applicationId) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(complianceMilestoneService.createMilestones(applicationId));
    }

    /**
     * Beneficiaries may only view milestones for their own application;
     * officers/finance/admin may view any application's milestones.
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<DisbursementMilestone>> getApplicationMilestones(
            @PathVariable Long applicationId, Authentication authentication) {

        assertCanViewApplication(applicationId, authentication);
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

    // ---------- Auth Helper ----------

    /**
     * Mirrors ApplicationService.getApplicationById's ownership check:
     * a BENEFICIARY may only see data for applications tied to their own
     * beneficiary profile; every other role passes through unrestricted.
     */
    private void assertCanViewApplication(Long applicationId, Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));

        if (currentUser.getRole() == Role.BENEFICIARY) {
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
            Beneficiary beneficiary = application.getBeneficiary();
            if (beneficiary.getUser() == null || beneficiary.getUser().getId() != currentUser.getId()) {
                throw new InvalidOperationException("You are not authorized to view this application's milestones.");
            }
        }
    }
}