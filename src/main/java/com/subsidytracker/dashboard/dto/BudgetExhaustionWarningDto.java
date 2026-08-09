package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matches Person 3's planned AnalyticsService#budgetExhaustionWarnings() response shape.
 * severity: "OK" (&lt;75%), "WARNING" (75-95%), "CRITICAL" (&gt;95%).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetExhaustionWarningDto {
    private String schemeName;
    private String regionName;
    private double utilizationPercent;
    private String severity;
}
