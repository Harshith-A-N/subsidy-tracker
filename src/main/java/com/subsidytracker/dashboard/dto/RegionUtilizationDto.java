package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Matches Person 3's planned AnalyticsService#fundUtilizationByRegion() response shape. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegionUtilizationDto {
    private String regionName;
    private BigDecimal allocatedBudget;
    private BigDecimal utilizedBudget;
    private double utilizationPercent;
    private long applicationCount;
    private long approvedCount;
}
