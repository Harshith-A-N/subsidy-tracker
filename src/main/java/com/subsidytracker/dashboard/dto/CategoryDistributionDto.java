package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matches Person 3's planned AnalyticsService#beneficiaryCategoryDistribution() response shape.
 * category corresponds to common.enums.BeneficiaryCategory (GENERAL, SC, ST, OBC, EWS).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDistributionDto {
    private String category;
    private long count;
    private double percent;
}
