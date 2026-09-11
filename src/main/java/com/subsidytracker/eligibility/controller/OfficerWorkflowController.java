package com.subsidytracker.eligibility.controller;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.common.enums.VerificationDecision;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.service.ApplicationService;
import com.subsidytracker.eligibility.service.VerificationService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OfficerWorkflowController {

    private final ApplicationService applicationService;
    private final VerificationService verificationService;
    private final UserRepository userRepository;

    public OfficerWorkflowController(ApplicationService applicationService,
                                     VerificationService verificationService,
                                     UserRepository userRepository) {
        this.applicationService = applicationService;
        this.verificationService = verificationService;
        this.userRepository = userRepository;
    }

    // ==========================================
    // 1. FIELD OFFICER WORKSPACE ENDPOINTS
    // ==========================================

    @GetMapping("/api/v1/field-verification/queue")
    public ResponseEntity<List<ApplicationResponseDto>> getFieldVerificationQueue(Authentication authentication) {
        String officerRegion = resolveUserRegion(authentication);
        List<ApplicationResponseDto> list = applicationService.getApplicationsByStatusInAndRegion(
                List.of(ApplicationStatus.FIELD_VERIFICATION_PENDING, ApplicationStatus.RE_VERIFICATION_REQUIRED),
                officerRegion
        );
        return ResponseEntity.ok(list);
    }

    @PostMapping("/api/v1/field-verification/{applicationId}/verify")
    public ResponseEntity<VerificationResponseDto> verifyFieldApplication(
            @PathVariable Long applicationId,
            @RequestBody OfficerActionRequest request,
            Authentication authentication) {
        long officerId = resolveUserId(authentication);
        VerificationRequestDto dto = new VerificationRequestDto();
        dto.setDecision(parseDecision(request.getStatus(), request.getDecision()));
        dto.setRemarks(request.getRemarks() != null ? request.getRemarks() : "Field inspection verified.");
        return ResponseEntity.ok(verificationService.processVerification(applicationId, dto, officerId));
    }

    // ==========================================
    // 2. DISTRICT OFFICER SANCTIONING DESK
    // ==========================================

    @GetMapping("/api/v1/district-officer/queue")
    public ResponseEntity<List<ApplicationResponseDto>> getDistrictOfficerQueue(Authentication authentication) {
        String officerRegion = resolveUserRegion(authentication);
        List<ApplicationResponseDto> list = applicationService.getApplicationsByStatusInAndRegion(
                List.of(ApplicationStatus.DISTRICT_REVIEW_PENDING, ApplicationStatus.FIELD_APPROVED),
                officerRegion
        );
        return ResponseEntity.ok(list);
    }

    @PostMapping("/api/v1/district-officer/{applicationId}/review")
    public ResponseEntity<VerificationResponseDto> reviewDistrictApplication(
            @PathVariable Long applicationId,
            @RequestBody OfficerActionRequest request,
            Authentication authentication) {
        long officerId = resolveUserId(authentication);
        VerificationRequestDto dto = new VerificationRequestDto();
        dto.setDecision(parseDecision(request.getStatus(), request.getDecision()));
        dto.setRemarks(request.getRemarks() != null ? request.getRemarks() : "District sanction review recorded.");
        return ResponseEntity.ok(verificationService.processVerification(applicationId, dto, officerId));
    }

    // ==========================================
    // 3. FINANCE & TREASURY DISBURSEMENT DESK
    // ==========================================

    @GetMapping("/api/v1/finance/queue")
    public ResponseEntity<List<ApplicationResponseDto>> getFinanceApprovalQueue(Authentication authentication) {
        List<ApplicationResponseDto> list = applicationService.getApplicationsByStatusIn(
                List.of(ApplicationStatus.FINANCE_REVIEW_PENDING, ApplicationStatus.DISTRICT_APPROVED)
        );
        return ResponseEntity.ok(list);
    }

    @PostMapping("/api/v1/finance/{applicationId}/review")
    public ResponseEntity<VerificationResponseDto> reviewFinanceApplication(
            @PathVariable Long applicationId,
            @RequestBody OfficerActionRequest request,
            Authentication authentication) {
        long officerId = resolveUserId(authentication);
        VerificationRequestDto dto = new VerificationRequestDto();
        dto.setDecision(parseDecision(request.getStatus(), request.getDecision()));
        dto.setRemarks(request.getRemarks() != null ? request.getRemarks() : "Finance disbursement authorization granted.");
        return ResponseEntity.ok(verificationService.processVerification(applicationId, dto, officerId));
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private VerificationDecision parseDecision(String status, String explicitDecision) {
        String val = explicitDecision != null ? explicitDecision : (status != null ? status : "APPROVE");
        val = val.toUpperCase();
        if (val.contains("REJECT")) {
            return VerificationDecision.REJECTED;
        } else if (val.contains("RE_VERIFY") || val.contains("RE_VERIFICATION")) {
            return VerificationDecision.RE_VERIFICATION_REQUESTED;
        } else {
            return VerificationDecision.APPROVED;
        }
    }

    private long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));
        return user.getId();
    }

    private String resolveUserRegion(Authentication authentication) {
        if (authentication == null) return null;
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(User::getRegion)
                .orElse(null);
    }

    @Getter
    @Setter
    public static class OfficerActionRequest {
        private String status;
        private String decision;
        private String remarks;
        private String gpsLocation;
    }
}
