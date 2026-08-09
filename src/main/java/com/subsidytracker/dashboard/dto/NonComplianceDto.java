package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Matches Person 3's planned AnalyticsService#nonComplianceAnalysis() response shape. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NonComplianceDto {
    private String schemeName;
    private String regionName;
    private long nonCompliantCount;
}
