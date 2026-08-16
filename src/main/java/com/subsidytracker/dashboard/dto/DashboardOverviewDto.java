package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Aggregate summary for the top stat-card row of the dashboard.
 * Not one of Person 3's seven named methods in the PDF, but a convenience rollup
 * Person 4 (dashboard/reports) is allowed to compose from the other six.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {
    private long totalApplications;
    private long approvedApplications;
    private long pendingApplications;
    private long rejectedApplications;
    private BigDecimal totalBudgetAllocated;
    private BigDecimal totalBudgetUtilized;
    private long overdueMilestones;
}
