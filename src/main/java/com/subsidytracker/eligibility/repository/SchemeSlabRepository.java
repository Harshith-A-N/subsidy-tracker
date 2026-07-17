package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.SchemeSlab;
import com.subsidytracker.common.enums.BeneficiaryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for SchemeSlabs.
 *
 * Used by Module 2's eligibility engine to look up the grant amount
 * for a given scheme + beneficiary category combination.
 */
@Repository
public interface SchemeSlabRepository extends JpaRepository<SchemeSlab, Long> {

    // Find all slabs for a scheme (useful for listing available grant tiers)
    List<SchemeSlab> findBySchemeId(Long schemeId);

    // Find the specific slab for a scheme and beneficiary category
    // This determines the grant amount the beneficiary is eligible for
    Optional<SchemeSlab> findBySchemeIdAndCategory(Long schemeId, BeneficiaryCategory category);
}
