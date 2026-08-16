package com.subsidytracker.analytics.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> getSchemeUtilizations() {
        String jpql = "SELECT s.id, s.name, s.totalBudget, " +
                      "COALESCE((SELECT SUM(ads.scheduledAmount) " +
                      "          FROM ApplicationDisbursementSchedule ads " +
                      "          WHERE ads.application.scheme.id = s.id " +
                      "            AND ads.status = com.subsidytracker.common.enums.DisbursementScheduleStatus.RELEASED), 0) " +
                      "FROM Scheme s " +
                      "WHERE s.isActive = true";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getRegionalBudgets() {
        String jpql = "SELECT rb.regionName, SUM(rb.allocatedBudget) " +
                      "FROM RegionalBudget rb " +
                      "GROUP BY rb.regionName";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getRegionApplicationCounts() {
        String jpql = "SELECT b.region, COUNT(a.id), " +
                      "SUM(CASE WHEN a.status = com.subsidytracker.common.enums.ApplicationStatus.READY_FOR_DISBURSEMENT " +
                      "             OR a.status = com.subsidytracker.common.enums.ApplicationStatus.DISBURSED " +
                      "             OR a.status = com.subsidytracker.common.enums.ApplicationStatus.COMPLETED " +
                      "         THEN 1.0 ELSE 0.0 END) " +
                      "FROM Application a " +
                      "JOIN a.beneficiary b " +
                      "GROUP BY b.region";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getRegionReleasedBudgets() {
        String jpql = "SELECT b.region, SUM(ads.scheduledAmount) " +
                      "FROM ApplicationDisbursementSchedule ads " +
                      "JOIN ads.application a " +
                      "JOIN a.beneficiary b " +
                      "WHERE ads.status = com.subsidytracker.common.enums.DisbursementScheduleStatus.RELEASED " +
                      "GROUP BY b.region";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getMilestoneCounts() {
        String jpql = "SELECT m.complianceStatus, COUNT(m.id) " +
                      "FROM DisbursementMilestone m " +
                      "GROUP BY m.complianceStatus";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getNonComplianceCounts() {
        String jpql = "SELECT a.scheme.name, b.region, COUNT(m.id) " +
                      "FROM DisbursementMilestone m " +
                      "JOIN m.application a " +
                      "JOIN a.beneficiary b " +
                      "WHERE m.complianceStatus = com.subsidytracker.common.enums.ComplianceStatus.OVERDUE " +
                      "   OR m.complianceStatus = com.subsidytracker.common.enums.ComplianceStatus.NON_COMPLIANT " +
                      "GROUP BY a.scheme.name, b.region";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getTurnaroundTimes() {
        String jpql = "SELECT a.submissionDate, v.verificationDate " +
                      "FROM Verification v " +
                      "JOIN v.application a " +
                      "WHERE v.level = com.subsidytracker.common.enums.VerificationLevel.FINANCE " +
                      "  AND v.decision = com.subsidytracker.common.enums.VerificationDecision.APPROVED";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getReleasedBudgetsBySchemeAndRegion() {
        String jpql = "SELECT a.scheme.id, b.region, SUM(ads.scheduledAmount) " +
                      "FROM ApplicationDisbursementSchedule ads " +
                      "JOIN ads.application a " +
                      "JOIN a.beneficiary b " +
                      "WHERE ads.status = com.subsidytracker.common.enums.DisbursementScheduleStatus.RELEASED " +
                      "GROUP BY a.scheme.id, b.region";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getApplicationStatusCounts() {
        String jpql = "SELECT a.status, COUNT(a.id) " +
                      "FROM Application a " +
                      "GROUP BY a.status";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<Object[]> getBeneficiaryCategoryDistribution() {
        String jpql = "SELECT b.category, COUNT(a.id) " +
                      "FROM Application a " +
                      "JOIN a.beneficiary b " +
                      "GROUP BY b.category";
        return entityManager.createQuery(jpql, Object[].class).getResultList();
    }

    public List<com.subsidytracker.common.entity.RegionalBudget> getAllRegionalBudgets() {
        String jpql = "SELECT rb FROM RegionalBudget rb JOIN FETCH rb.scheme";
        return entityManager.createQuery(jpql, com.subsidytracker.common.entity.RegionalBudget.class).getResultList();
    }
}
