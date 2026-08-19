package com.subsidytracker.dashboard.service;

import java.util.List;

import com.subsidytracker.dashboard.dto.ApprovalTurnaroundDto;
import com.subsidytracker.dashboard.dto.BudgetExhaustionWarningDto;
import com.subsidytracker.dashboard.dto.CategoryDistributionDto;
import com.subsidytracker.dashboard.dto.DashboardOverviewDto;
import com.subsidytracker.dashboard.dto.NonComplianceDto;
import com.subsidytracker.dashboard.dto.PendingMilestoneSummaryDto;
import com.subsidytracker.dashboard.dto.RegionUtilizationDto;
import com.subsidytracker.dashboard.dto.SchemeUtilizationDto;

/**
 * Contract for wherever the dashboard/reports layer gets its numbers from.
 *
 * The real implementation is {@link RealAnalyticsDataSourceAdapter}, a thin
 * @Primary adapter over com.subsidytracker.analytics.service.AnalyticsService.
 * This interface exists so the dashboard/reports controllers depend only on
 * the contract, not on that adapter directly.
 *
 * Method names mirror the analytics package's real query methods
 * (fundUtilizationByScheme, fundUtilizationByRegion, etc.) so the mapping
 * between the two stays close to 1:1.
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