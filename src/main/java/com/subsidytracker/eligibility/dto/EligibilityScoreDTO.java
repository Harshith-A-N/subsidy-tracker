package com.subsidytracker.eligibility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after eligibility scoring.
 *
 * Contains the calculated score, resulting application status,
 * and remarks explaining the scoring decision.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityScoreDTO {
    private double eligibilityScore;
    private String eligibilityStatus;
    private String remarks;
}
