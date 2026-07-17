package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access layer for Scheme entities.
 *
 * Used by Module 2's eligibility engine to load scheme details
 * (income limits, allowed categories) for scoring.
 */
@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {
    // JpaRepository provides findById(), findAll(), save(), etc. out of the box.
    // No custom queries needed for Module 2 at this time.
}
