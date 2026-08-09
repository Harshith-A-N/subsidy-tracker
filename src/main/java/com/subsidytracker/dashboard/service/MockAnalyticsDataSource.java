package com.subsidytracker.dashboard.service;

import com.subsidytracker.dashboard.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================================
 *  MOCK DATA — TEMPORARY. Delete this class (or stop using it) once Person 3's
 *  real com.subsidytracker.analytics.service.AnalyticsService is merged.
 *
 *  Numbers below are hand-picked to look realistic against the shared example
 *  scheme ("Solar Pump Subsidy") agreed in Section 4.2 of the Milestone 3 doc,
 *  plus two more schemes for variety. Swap procedure:
 *    1. Implement AnalyticsDataSource in the analytics package (or a small
 *       adapter that calls Person 3's AnalyticsService).
 *    2. Annotate that new bean @Primary.
 *    3. Either delete this class or remove its @Service annotation so Spring
 *       doesn't see two candidate beans.
 *  No controller code changes needed — they depend on the interface only.
 * ============================================================================
 */
@Service
public class MockAnalyticsDataSource implements AnalyticsDataSource {

    @Override
    public DashboardOverviewDto overview() {
        return new DashboardOverviewDto(
                428,                          // totalApplications
                261,                          // approvedApplications
                97,                           // pendingApplications
                70,                           // rejectedApplications
                new BigDecimal("50000000"),   // totalBudgetAllocated
                new BigDecimal("28650000"),   // totalBudgetUtilized
                14                            // overdueMilestones
        );
    }

    @Override
    public List<SchemeUtilizationDto> fundUtilizationByScheme() {
        return List.of(
                new SchemeUtilizationDto(1L, "Solar Pump Subsidy",
                        new BigDecimal("20000000"), new BigDecimal("13400000"), 67.0),
                new SchemeUtilizationDto(2L, "Rural Housing Grant",
                        new BigDecimal("18000000"), new BigDecimal("9250000"), 51.4),
                new SchemeUtilizationDto(3L, "Farmer Equipment Subsidy",
                        new BigDecimal("12000000"), new BigDecimal("6000000"), 50.0)
        );
    }

    @Override
    public List<RegionUtilizationDto> fundUtilizationByRegion() {
        return List.of(
                new RegionUtilizationDto("Madurai", new BigDecimal("10000000"), new BigDecimal("8700000"), 87.0, 112, 74),
                new RegionUtilizationDto("Coimbatore", new BigDecimal("9000000"), new BigDecimal("6100000"), 67.8, 96, 61),
                new RegionUtilizationDto("Trichy", new BigDecimal("8500000"), new BigDecimal("5200000"), 61.2, 88, 52),
                new RegionUtilizationDto("Tirunelveli", new BigDecimal("7500000"), new BigDecimal("3650000"), 48.7, 74, 41),
                new RegionUtilizationDto("Salem", new BigDecimal("6000000"), new BigDecimal("2900000"), 48.3, 58, 33)
        );
    }

    @Override
    public PendingMilestoneSummaryDto pendingMilestoneSummary() {
        return new PendingMilestoneSummaryDto(53, 14, 187);
    }

    @Override
    public List<NonComplianceDto> nonComplianceAnalysis() {
        return List.of(
                new NonComplianceDto("Solar Pump Subsidy", "Madurai", 6),
                new NonComplianceDto("Rural Housing Grant", "Salem", 4),
                new NonComplianceDto("Farmer Equipment Subsidy", "Tirunelveli", 4)
        );
    }

    @Override
    public ApprovalTurnaroundDto approvalTurnaroundTime() {
        return new ApprovalTurnaroundDto(9.4, 2.0, 26.0);
    }

    @Override
    public List<BudgetExhaustionWarningDto> budgetExhaustionWarnings() {
        return List.of(
                new BudgetExhaustionWarningDto("Solar Pump Subsidy", "Madurai", 87.0, "CRITICAL"),
                new BudgetExhaustionWarningDto("Rural Housing Grant", "Coimbatore", 78.4, "WARNING"),
                new BudgetExhaustionWarningDto("Farmer Equipment Subsidy", "Trichy", 61.2, "OK")
        );
    }

    @Override
    public List<CategoryDistributionDto> beneficiaryCategoryDistribution() {
        return List.of(
                new CategoryDistributionDto("GENERAL", 118, 27.6),
                new CategoryDistributionDto("SC", 104, 24.3),
                new CategoryDistributionDto("ST", 61, 14.2),
                new CategoryDistributionDto("OBC", 97, 22.7),
                new CategoryDistributionDto("EWS", 48, 11.2)
        );
    }
}
