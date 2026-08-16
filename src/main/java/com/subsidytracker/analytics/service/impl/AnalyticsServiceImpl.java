package com.subsidytracker.analytics.service.impl;

import com.subsidytracker.analytics.repository.AnalyticsRepository;
import com.subsidytracker.analytics.service.AnalyticsService;
import com.subsidytracker.common.entity.RegionalBudget;
import com.subsidytracker.common.enums.BeneficiaryCategory;
import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.dashboard.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository repository;

    public AnalyticsServiceImpl(AnalyticsRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SchemeUtilizationDto> fundUtilizationByScheme() {
        List<Object[]> schemeList = repository.getSchemeUtilizations();
        List<SchemeUtilizationDto> result = new ArrayList<>();

        for (Object[] row : schemeList) {
            Long schemeId = (Long) row[0];
            String schemeName = (String) row[1];
            BigDecimal totalBudget = (BigDecimal) row[2];
            BigDecimal utilizedBudget = (BigDecimal) row[3];

            double utilizationPercent = 0.0;
            if (totalBudget != null && totalBudget.compareTo(BigDecimal.ZERO) > 0) {
                utilizationPercent = utilizedBudget.multiply(new BigDecimal("100"))
                        .divide(totalBudget, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(new SchemeUtilizationDto(schemeId, schemeName, totalBudget, utilizedBudget, utilizationPercent));
        }

        return result;
    }

    @Override
    public List<RegionUtilizationDto> fundUtilizationByRegion() {
        Set<String> allRegions = new TreeSet<>();
        Map<String, BigDecimal> allocatedMap = new HashMap<>();
        Map<String, Long> totalAppMap = new HashMap<>();
        Map<String, Long> approvedAppMap = new HashMap<>();
        Map<String, BigDecimal> utilizedMap = new HashMap<>();

        // 1. Allocated budgets
        List<Object[]> allocatedList = repository.getRegionalBudgets();
        for (Object[] row : allocatedList) {
            String region = (String) row[0];
            BigDecimal allocated = (BigDecimal) row[1];
            if (region != null) {
                allRegions.add(region);
                allocatedMap.put(region, allocated);
            }
        }

        // 2. Application and Approved counts
        List<Object[]> countsList = repository.getRegionApplicationCounts();
        for (Object[] row : countsList) {
            String region = (String) row[0];
            Long totalCount = ((Number) row[1]).longValue();
            Long approvedCount = ((Number) row[2]).longValue();
            if (region != null) {
                allRegions.add(region);
                totalAppMap.put(region, totalCount);
                approvedAppMap.put(region, approvedCount);
            }
        }

        // 3. Utilized budget (released amount)
        List<Object[]> utilizedList = repository.getRegionReleasedBudgets();
        for (Object[] row : utilizedList) {
            String region = (String) row[0];
            BigDecimal utilized = (BigDecimal) row[1];
            if (region != null) {
                allRegions.add(region);
                utilizedMap.put(region, utilized);
            }
        }

        // 4. Combine into RegionUtilizationDto list
        List<RegionUtilizationDto> result = new ArrayList<>();
        for (String region : allRegions) {
            BigDecimal allocated = allocatedMap.getOrDefault(region, BigDecimal.ZERO);
            BigDecimal utilized = utilizedMap.getOrDefault(region, BigDecimal.ZERO);
            long totalCount = totalAppMap.getOrDefault(region, 0L);
            long approvedCount = approvedAppMap.getOrDefault(region, 0L);

            double utilizationPercent = 0.0;
            if (allocated != null && allocated.compareTo(BigDecimal.ZERO) > 0) {
                utilizationPercent = utilized.multiply(new BigDecimal("100"))
                        .divide(allocated, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(new RegionUtilizationDto(region, allocated, utilized, utilizationPercent, totalCount, approvedCount));
        }

        return result;
    }

    @Override
    public PendingMilestoneSummaryDto pendingMilestoneSummary() {
        long pending = 0;
        long overdue = 0;
        long completed = 0;

        List<Object[]> counts = repository.getMilestoneCounts();
        for (Object[] row : counts) {
            ComplianceStatus status = (ComplianceStatus) row[0];
            long count = ((Number) row[1]).longValue();

            if (status == ComplianceStatus.PENDING) {
                pending = count;
            } else if (status == ComplianceStatus.OVERDUE) {
                overdue = count;
            } else if (status == ComplianceStatus.COMPLETED) {
                completed = count;
            }
        }

        return new PendingMilestoneSummaryDto(pending, overdue, completed);
    }

    @Override
    public List<NonComplianceDto> nonComplianceAnalysis() {
        List<NonComplianceDto> result = new ArrayList<>();
        List<Object[]> list = repository.getNonComplianceCounts();

        for (Object[] row : list) {
            String schemeName = (String) row[0];
            String regionName = (String) row[1];
            long count = ((Number) row[2]).longValue();

            result.add(new NonComplianceDto(schemeName, regionName, count));
        }

        return result;
    }

    @Override
    public ApprovalTurnaroundDto approvalTurnaroundTime() {
        List<Object[]> times = repository.getTurnaroundTimes();
        if (times.isEmpty()) {
            return new ApprovalTurnaroundDto(0.0, 0.0, 0.0);
        }

        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (Object[] row : times) {
            LocalDate submissionDate = (LocalDate) row[0];
            LocalDateTime verificationDate = (LocalDateTime) row[1];
            long days = ChronoUnit.DAYS.between(submissionDate, verificationDate.toLocalDate());
            double daysDouble = (double) days;

            sum += daysDouble;
            if (daysDouble < min) {
                min = daysDouble;
            }
            if (daysDouble > max) {
                max = daysDouble;
            }
        }

        double average = sum / times.size();

        average = Math.round(average * 10.0) / 10.0;
        min = Math.round(min * 10.0) / 10.0;
        max = Math.round(max * 10.0) / 10.0;

        return new ApprovalTurnaroundDto(average, min, max);
    }

    @Override
    public List<BudgetExhaustionWarningDto> budgetExhaustionWarnings() {
        List<BudgetExhaustionWarningDto> warnings = new ArrayList<>();
        List<RegionalBudget> budgets = repository.getAllRegionalBudgets();

        Map<String, BigDecimal> releasedMap = new HashMap<>();
        List<Object[]> releasedList = repository.getReleasedBudgetsBySchemeAndRegion();
        for (Object[] row : releasedList) {
            Long schemeId = (Long) row[0];
            String region = (String) row[1];
            BigDecimal releasedSum = (BigDecimal) row[2];

            if (schemeId != null && region != null) {
                releasedMap.put(schemeId + "_" + region, releasedSum);
            }
        }

        for (RegionalBudget rb : budgets) {
            BigDecimal allocated = rb.getAllocatedBudget();
            BigDecimal released = releasedMap.getOrDefault(rb.getScheme().getId() + "_" + rb.getRegionName(), BigDecimal.ZERO);

            double utilizationPercent = 0.0;
            if (allocated != null && allocated.compareTo(BigDecimal.ZERO) > 0) {
                utilizationPercent = released.multiply(new BigDecimal("100"))
                        .divide(allocated, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            String severity = "OK";
            if (utilizationPercent > 95.0) {
                severity = "CRITICAL";
            } else if (utilizationPercent >= 75.0) {
                severity = "WARNING";
            }

            warnings.add(new BudgetExhaustionWarningDto(rb.getScheme().getName(), rb.getRegionName(), utilizationPercent, severity));
        }

        return warnings;
    }

    @Override
    public List<CategoryDistributionDto> beneficiaryCategoryDistribution() {
        List<Object[]> counts = repository.getBeneficiaryCategoryDistribution();
        Map<BeneficiaryCategory, Long> categoryCounts = new HashMap<>();

        for (BeneficiaryCategory cat : BeneficiaryCategory.values()) {
            categoryCounts.put(cat, 0L);
        }

        long total = 0;
        for (Object[] row : counts) {
            BeneficiaryCategory cat = (BeneficiaryCategory) row[0];
            long count = ((Number) row[1]).longValue();

            if (cat != null) {
                categoryCounts.put(cat, count);
                total += count;
            }
        }

        List<CategoryDistributionDto> result = new ArrayList<>();
        for (BeneficiaryCategory cat : BeneficiaryCategory.values()) {
            long count = categoryCounts.get(cat);
            double percent = 0.0;

            if (total > 0) {
                percent = (double) count * 100.0 / total;
                percent = Math.round(percent * 10.0) / 10.0;
            }

            result.add(new CategoryDistributionDto(cat.name(), count, percent));
        }

        return result;
    }

    @Override
    public DashboardOverviewDto overview() {
        long totalApplications = 0;
        long approvedApplications = 0;
        long pendingApplications = 0;
        long rejectedApplications = 0;

        List<Object[]> statusCounts = repository.getApplicationStatusCounts();
        for (Object[] row : statusCounts) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalApplications += count;

            if (status == ApplicationStatus.READY_FOR_DISBURSEMENT
                    || status == ApplicationStatus.DISBURSED
                    || status == ApplicationStatus.COMPLETED) {
                approvedApplications += count;
            } else if (status == ApplicationStatus.FIELD_REJECTED
                    || status == ApplicationStatus.DISTRICT_REJECTED
                    || status == ApplicationStatus.FINANCE_REJECTED
                    || status == ApplicationStatus.NOT_ELIGIBLE
                    || status == ApplicationStatus.APPLICATION_CANCELLED) {
                rejectedApplications += count;
            } else {
                pendingApplications += count;
            }
        }

        BigDecimal totalBudgetAllocated = BigDecimal.ZERO;
        List<Object[]> schemeList = repository.getSchemeUtilizations();
        for (Object[] row : schemeList) {
            BigDecimal budget = (BigDecimal) row[2];
            if (budget != null) {
                totalBudgetAllocated = totalBudgetAllocated.add(budget);
            }
        }

        BigDecimal totalBudgetUtilized = BigDecimal.ZERO;
        List<Object[]> regionalReleased = repository.getRegionReleasedBudgets();
        for (Object[] row : regionalReleased) {
            BigDecimal released = (BigDecimal) row[1];
            if (released != null) {
                totalBudgetUtilized = totalBudgetUtilized.add(released);
            }
        }

        long overdueMilestones = 0;
        List<Object[]> milestones = repository.getMilestoneCounts();
        for (Object[] row : milestones) {
            ComplianceStatus status = (ComplianceStatus) row[0];
            if (status == ComplianceStatus.OVERDUE) {
                overdueMilestones = ((Number) row[1]).longValue();
            }
        }

        return new DashboardOverviewDto(
                totalApplications,
                approvedApplications,
                pendingApplications,
                rejectedApplications,
                totalBudgetAllocated,
                totalBudgetUtilized,
                overdueMilestones
        );
    }
}
