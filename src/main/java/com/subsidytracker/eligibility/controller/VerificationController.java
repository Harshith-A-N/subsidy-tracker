package com.subsidytracker.eligibility.controller;

import com.subsidytracker.eligibility.dto.VerificationRequestDTO;
import com.subsidytracker.eligibility.dto.VerificationResponseDTO;
import com.subsidytracker.eligibility.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/{applicationId}/verify")
    public ResponseEntity<VerificationResponseDTO> processVerification(
            @PathVariable Long applicationId,
            @RequestBody VerificationRequestDTO request) {
        
        VerificationResponseDTO response = verificationService.processVerification(applicationId, request);
        return ResponseEntity.ok(response);
    }
}
