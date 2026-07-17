package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.Verification;
import com.subsidytracker.common.enums.VerificationLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for Verification records.
 *
 * Used by Module 2 to:
 * - Retrieve the full verification history of an application
 * - Check if a specific verification level has already been completed
 */
@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {

    // Get complete verification history for an application, ordered by date
    List<Verification> findByApplicationIdOrderByVerificationDateAsc(Long applicationId);

    // Check if a verification at a specific level already exists for an application
    boolean existsByApplicationIdAndLevel(Long applicationId, VerificationLevel level);
}
