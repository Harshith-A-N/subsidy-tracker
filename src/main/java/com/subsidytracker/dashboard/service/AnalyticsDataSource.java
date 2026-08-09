package com.subsidytracker.dashboard.service;

import com.subsidytracker.dashboard.dto.*;

import java.util.List;

/**
 * Contract for wherever the dashboard/reports layer gets its numbers from.
 *
 * This exists so Person 4 (dashboard + reports) never has to wait on Person 3
 * (analytics). Build and demo everything against {@link MockAnalyticsDataSource}
 * now; once Person 3's real com.subsidytracker.analytics.service.AnalyticsService
 * is merged, add a thin adapter class that implements this interface by delegating
 * to it, mark it @Primary, and every controller in this package keeps working
 * unchanged.
 *
 * Method names mirror the seven methods listed for Person 3 in Section 3 of the
 * Milestone 3 doc (fundUtilizationByScheme, fundUtilizationByRegion, etc.) so the
 * eventual adapter is close to a 1:1 mapping.
 */
public interface AnalyticsDataSource {

    DashboardOverviewDto overview();

    List<SchemeUtilizationDto> fundUtilizationByScheme();

    List<RegionUtilizationDto> fundUtilizationByRegion();

    PendingMilestoneSummaryDto pendingMilestoneSummary();

    List<NonComplianceDto> nonComplianceAnalysis();

    ApprovalTurnaroundDto approvalTurnaroundTime();

    List<BudgetExhaustionWarningDto> budgetExhaustionWarnings();

    List<CategoryDistributionDto> beneficiaryCategoryDistribution();
}
