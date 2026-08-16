package com.subsidytracker.dashboard.service;

import com.subsidytracker.analytics.service.AnalyticsService;
import com.subsidytracker.dashboard.dto.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class RealAnalyticsDataSourceAdapter implements AnalyticsDataSource {

    private final AnalyticsService analyticsService;

    public RealAnalyticsDataSourceAdapter(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public DashboardOverviewDto overview() {
        return analyticsService.overview();
    }

    @Override
    public List<SchemeUtilizationDto> fundUtilizationByScheme() {
        return analyticsService.fundUtilizationByScheme();
    }

    @Override
    public List<RegionUtilizationDto> fundUtilizationByRegion() {
        return analyticsService.fundUtilizationByRegion();
    }

    @Override
    public PendingMilestoneSummaryDto pendingMilestoneSummary() {
        return analyticsService.pendingMilestoneSummary();
    }

    @Override
    public List<NonComplianceDto> nonComplianceAnalysis() {
        return analyticsService.nonComplianceAnalysis();
    }

    @Override
    public ApprovalTurnaroundDto approvalTurnaroundTime() {
        return analyticsService.approvalTurnaroundTime();
    }

    @Override
    public List<BudgetExhaustionWarningDto> budgetExhaustionWarnings() {
        return analyticsService.budgetExhaustionWarnings();
    }

    @Override
    public List<CategoryDistributionDto> beneficiaryCategoryDistribution() {
        return analyticsService.beneficiaryCategoryDistribution();
    }
}
