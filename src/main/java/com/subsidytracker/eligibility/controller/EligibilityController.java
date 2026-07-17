package com.subsidytracker.eligibility.controller;

import com.subsidytracker.eligibility.dto.EligibilityScoreDTO;
import com.subsidytracker.eligibility.service.EligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for eligibility scoring operations.
 *
 * Follows the project's "thin controller" convention:
 * - No business logic here — everything is delegated to EligibilityService.
 * - The controller only handles HTTP concerns (request mapping, response wrapping).
 */
@RestController
@RequestMapping("/api/applications")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    // Constructor injection
    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Triggers eligibility scoring for a specific application.
     *
     * What happens:
     * 1. Reads the beneficiary profile and scheme rules
     * 2. Calculates a score (0–100)
     * 3. If score >= 40: advances application to PENDING_FIELD_REVIEW
     * 4. If score < 40: rejects the application
     *
     * @param applicationId the ID of the application to score
     * @return EligibilityScoreDTO with detailed scoring breakdown
     */
    @PostMapping("/{applicationId}/calculate-eligibility")
    public ResponseEntity<EligibilityScoreDTO> calculateEligibility(
            @PathVariable Long applicationId) {

        EligibilityScoreDTO result = eligibilityService.calculateEligibility(applicationId);
        return ResponseEntity.ok(result);
    }
}
