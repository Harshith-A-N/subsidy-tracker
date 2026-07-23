package com.subsidytracker.eligibility.controller;

import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.service.EligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @PostMapping("/{applicationId}/calculate-eligibility")
    public ResponseEntity<ApplicationResponseDto> calculate(@PathVariable Long applicationId) {
        return ResponseEntity.ok(eligibilityService.calculateEligibility(applicationId));
    }
}