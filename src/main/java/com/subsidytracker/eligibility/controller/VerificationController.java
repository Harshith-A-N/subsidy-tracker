package com.subsidytracker.eligibility.controller;

import com.subsidytracker.eligibility.dto.VerificationRequestDto;
import com.subsidytracker.eligibility.dto.VerificationResponseDto;
import com.subsidytracker.eligibility.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PatchMapping("/{applicationId}/verify")
    public ResponseEntity<VerificationResponseDto> verify(@PathVariable Long applicationId,
                                                          @RequestBody VerificationRequestDto request) {
        return ResponseEntity.ok(verificationService.processVerification(applicationId, request));
    }

    @PatchMapping("/{applicationId}/resume-verification")
    public ResponseEntity<VerificationResponseDto> resume(@PathVariable Long applicationId) {
        return ResponseEntity.ok(verificationService.resumeAfterReVerification(applicationId));
    }
}