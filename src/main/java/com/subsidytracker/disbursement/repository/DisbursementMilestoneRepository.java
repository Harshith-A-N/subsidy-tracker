package com.subsidytracker.disbursement.repository;

import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisbursementMilestoneRepository
        extends JpaRepository<DisbursementMilestone, Long> {

    List<DisbursementMilestone> findByApplicationIdOrderBySequenceOrderAsc(
            Long applicationId);

    List<DisbursementMilestone> findByComplianceStatus(
            ComplianceStatus status);

    List<DisbursementMilestone> findByComplianceStatusAndDueDateBefore(
            ComplianceStatus status,
            LocalDate date);

    boolean existsByApplicationIdAndSequenceOrder(
            Long applicationId,
            int sequenceOrder);
}