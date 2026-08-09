package com.subsidytracker.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matches Person 3's planned AnalyticsService#pendingMilestoneSummary() response shape.
 * Person 3 reads this from Person 2's ComplianceMilestone table (status field).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingMilestoneSummaryDto {
    private long pendingCount;
    private long overdueCount;
    private long completedCount;
}
