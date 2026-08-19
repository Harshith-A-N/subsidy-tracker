# Module 3 - Fund Utilization & Regional Analytics API Documentation

This module turning the data produced by Modules 1-2 and by Persons 1-2 into key metrics for officials: how much of each scheme's budget has been used, which regions are close to exhausting their allocation, and where compliance is breaking down.

## REST API Endpoints

All endpoints are protected under the base security policy and require a valid Bearer Token in the `Authorization` header.

### 1. Dashboard Overview Rollup
- **URL**: `/api/v1/analytics/overview`
- **Method**: `GET`
- **Response**: `JSON` (DashboardOverviewDto)
  ```json
  {
    "totalApplications": 428,
    "approvedApplications": 261,
    "pendingApplications": 97,
    "rejectedApplications": 70,
    "totalBudgetAllocated": 50000000.00,
    "totalBudgetUtilized": 28650000.00,
    "overdueMilestones": 14
  }
  ```

### 2. Fund Utilization By Scheme
- **URL**: `/api/v1/analytics/fund-utilization/schemes`
- **Method**: `GET`
- **Response**: `JSON` (List<SchemeUtilizationDto>)
  ```json
  [
    {
      "schemeId": 1,
      "schemeName": "Solar Pump Subsidy",
      "totalBudget": 20000000.00,
      "utilizedBudget": 13400000.00,
      "utilizationPercent": 67.0
    }
  ]
  ```

### 3. Fund Utilization By Region
- **URL**: `/api/v1/analytics/fund-utilization/regions`
- **Method**: `GET`
- **Response**: `JSON` (List<RegionUtilizationDto>)
  ```json
  [
    {
      "regionName": "Madurai",
      "allocatedBudget": 10000000.00,
      "utilizedBudget": 8700000.00,
      "utilizationPercent": 87.0,
      "applicationCount": 112,
      "approvedCount": 74
    }
  ]
  ```

### 4. Pending Milestone Summary
- **URL**: `/api/v1/analytics/compliance/pending-milestones`
- **Method**: `GET`
- **Response**: `JSON` (PendingMilestoneSummaryDto)
  ```json
  {
    "pendingCount": 53,
    "overdueCount": 14,
    "completedCount": 187
  }
  ```

### 5. Non-Compliance Analysis
- **URL**: `/api/v1/analytics/compliance/non-compliance`
- **Method**: `GET`
- **Response**: `JSON` (List<NonComplianceDto>)
  ```json
  [
    {
      "schemeName": "Solar Pump Subsidy",
      "regionName": "Madurai",
      "nonCompliantCount": 6
    }
  ]
  ```

### 6. Approval Turnaround Time
- **URL**: `/api/v1/analytics/approval-turnaround`
- **Method**: `GET`
- **Response**: `JSON` (ApprovalTurnaroundDto)
  ```json
  {
    "averageDays": 9.4,
    "fastestDays": 2.0,
    "slowestDays": 26.0
  }
  ```

### 7. Budget Exhaustion Warnings
- **URL**: `/api/v1/analytics/budget-exhaustion-warnings`
- **Method**: `GET`
- **Response**: `JSON` (List<BudgetExhaustionWarningDto>)
  ```json
  [
    {
      "schemeName": "Solar Pump Subsidy",
      "regionName": "Madurai",
      "utilizationPercent": 87.0,
      "severity": "CRITICAL"
    }
  ]
  ```

### 8. Beneficiary Category Distribution
- **URL**: `/api/v1/analytics/beneficiary-category-distribution`
- **Method**: `GET`
- **Response**: `JSON` (List<CategoryDistributionDto>)
  ```json
  [
    {
      "category": "GENERAL",
      "count": 118,
      "percent": 27.6
    }
  ]
  ```

## Query Optimization Details

- **No N+1 queries**: Aggregations are calculated using single native JPQL `GROUP BY` and join statements.
- **Java mapping**: Turnaround date difference math and percentages are calculated in Java for maximum database portability (tested on H2, runs on MySQL).
