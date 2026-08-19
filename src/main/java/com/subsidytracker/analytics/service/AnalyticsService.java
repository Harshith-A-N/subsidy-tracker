package com.subsidytracker.analytics.service;

import com.subsidytracker.dashboard.dto.*;

import java.util.List;

public interface AnalyticsService {

    List<SchemeUtilizationDto> fundUtilizationByScheme();

    List<RegionUtilizationDto> fundUtilizationByRegion();

    PendingMilestoneSummaryDto pendingMilestoneSummary();

    List<NonComplianceDto> nonComplianceAnalysis();

    ApprovalTurnaroundDto approvalTurnaroundTime();

    List<BudgetExhaustionWarningDto> budgetExhaustionWarnings();

    List<CategoryDistributionDto> beneficiaryCategoryDistribution();

    DashboardOverviewDto overview();
}
