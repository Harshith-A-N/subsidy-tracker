package com.subsidytracker.dashboard.controller;

import com.subsidytracker.dashboard.dto.*;
import com.subsidytracker.dashboard.service.AnalyticsDataSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Person 4 (Module 4 - Dashboard & Reports).
 * Read-only endpoints for the dashboard UI. Backed by AnalyticsDataSource,
 * currently MockAnalyticsDataSource — see that class for the swap-to-real
 * procedure once Person 3's analytics module is merged.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final AnalyticsDataSource analyticsDataSource;

    public DashboardController(AnalyticsDataSource analyticsDataSource) {
        this.analyticsDataSource = analyticsDataSource;
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDto> overview() {
        return ResponseEntity.ok(analyticsDataSource.overview());
    }

    @GetMapping("/fund-utilization/schemes")
    public ResponseEntity<List<SchemeUtilizationDto>> fundUtilizationByScheme() {
        return ResponseEntity.ok(analyticsDataSource.fundUtilizationByScheme());
    }

    @GetMapping("/fund-utilization/regions")
    public ResponseEntity<List<RegionUtilizationDto>> fundUtilizationByRegion() {
        return ResponseEntity.ok(analyticsDataSource.fundUtilizationByRegion());
    }

    @GetMapping("/compliance/pending-milestones")
    public ResponseEntity<PendingMilestoneSummaryDto> pendingMilestoneSummary() {
        return ResponseEntity.ok(analyticsDataSource.pendingMilestoneSummary());
    }

    @GetMapping("/compliance/non-compliance")
    public ResponseEntity<List<NonComplianceDto>> nonComplianceAnalysis() {
        return ResponseEntity.ok(analyticsDataSource.nonComplianceAnalysis());
    }

    @GetMapping("/approval-turnaround")
    public ResponseEntity<ApprovalTurnaroundDto> approvalTurnaroundTime() {
        return ResponseEntity.ok(analyticsDataSource.approvalTurnaroundTime());
    }

    @GetMapping("/budget-exhaustion-warnings")
    public ResponseEntity<List<BudgetExhaustionWarningDto>> budgetExhaustionWarnings() {
        return ResponseEntity.ok(analyticsDataSource.budgetExhaustionWarnings());
    }

    @GetMapping("/beneficiary-category-distribution")
    public ResponseEntity<List<CategoryDistributionDto>> beneficiaryCategoryDistribution() {
        return ResponseEntity.ok(analyticsDataSource.beneficiaryCategoryDistribution());
    }
}
