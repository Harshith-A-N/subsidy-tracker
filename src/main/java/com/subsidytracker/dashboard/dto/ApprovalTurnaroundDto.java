package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matches Person 3's planned AnalyticsService#approvalTurnaroundTime() response shape.
 * Computed from Module 2's application timestamps (submissionDate -> final approval).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTurnaroundDto {
    private double averageDays;
    private double fastestDays;
    private double slowestDays;
}
