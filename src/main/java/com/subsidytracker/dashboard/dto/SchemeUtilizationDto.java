package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Matches Person 3's planned AnalyticsService#fundUtilizationByScheme() response shape.
 * Field names agreed in Section 4.2 of the Milestone 3 doc — do not rename without
 * telling Person 3, since the real implementation must return this same shape.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchemeUtilizationDto {
    private Long schemeId;
    private String schemeName;
    private BigDecimal totalBudget;
    private BigDecimal utilizedBudget;
    private double utilizationPercent;
}
