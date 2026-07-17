package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for Application entities.
 *
 * Used by Module 2 to:
 * - Load an application for eligibility scoring
 * - Find applications by status (e.g., all PENDING_FIELD_REVIEW)
 * - Find applications by current stage
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Find all applications with a given status (e.g., PENDING_FIELD_REVIEW)
    List<Application> findByStatus(ApplicationStatus status);

    // Find all applications at a specific workflow stage
    List<Application> findByCurrentStage(String currentStage);

    // Find all applications for a specific beneficiary
    List<Application> findByBeneficiaryId(Long beneficiaryId);
}
