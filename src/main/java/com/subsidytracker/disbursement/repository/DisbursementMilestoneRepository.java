package com.subsidytracker.disbursement.repository;

import com.subsidytracker.common.enums.ComplianceStatus;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DisbursementMilestoneRepository
        extends JpaRepository<DisbursementMilestone, Long> {

    List<DisbursementMilestone> findByApplicationIdOrderBySequenceOrderAsc(
            Long applicationId);

    Page<DisbursementMilestone> findByApplicationIdOrderBySequenceOrderAsc(
            Long applicationId, Pageable pageable);

    List<DisbursementMilestone> findByComplianceStatus(
            ComplianceStatus status);

    Page<DisbursementMilestone> findByComplianceStatus(
            ComplianceStatus status, Pageable pageable);

    List<DisbursementMilestone> findByComplianceStatusAndDueDateBefore(
            ComplianceStatus status,
            LocalDate date);
}