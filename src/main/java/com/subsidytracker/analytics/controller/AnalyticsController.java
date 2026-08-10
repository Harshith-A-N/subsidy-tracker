package com.subsidytracker.analytics.controller;

import com.subsidytracker.analytics.service.AnalyticsService;
import com.subsidytracker.dashboard.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDto> overview() {
        return ResponseEntity.ok(analyticsService.overview());
    }

    @GetMapping("/fund-utilization/schemes")
    public ResponseEntity<List<SchemeUtilizationDto>> fundUtilizationByScheme() {
        return ResponseEntity.ok(analyticsService.fundUtilizationByScheme());
    }

    @GetMapping("/fund-utilization/regions")
    public ResponseEntity<List<RegionUtilizationDto>> fundUtilizationByRegion() {
        return ResponseEntity.ok(analyticsService.fundUtilizationByRegion());
    }

    @GetMapping("/compliance/pending-milestones")
    public ResponseEntity<PendingMilestoneSummaryDto> pendingMilestoneSummary() {
        return ResponseEntity.ok(analyticsService.pendingMilestoneSummary());
    }

    @GetMapping("/compliance/non-compliance")
    public ResponseEntity<List<NonComplianceDto>> nonComplianceAnalysis() {
        return ResponseEntity.ok(analyticsService.nonComplianceAnalysis());
    }

    @GetMapping("/approval-turnaround")
    public ResponseEntity<ApprovalTurnaroundDto> approvalTurnaroundTime() {
        return ResponseEntity.ok(analyticsService.approvalTurnaroundTime());
    }

    @GetMapping("/budget-exhaustion-warnings")
    public ResponseEntity<List<BudgetExhaustionWarningDto>> budgetExhaustionWarnings() {
        return ResponseEntity.ok(analyticsService.budgetExhaustionWarnings());
    }

    @GetMapping("/beneficiary-category-distribution")
    public ResponseEntity<List<CategoryDistributionDto>> beneficiaryCategoryDistribution() {
        return ResponseEntity.ok(analyticsService.beneficiaryCategoryDistribution());
    }
}
