package com.subsidytracker.scheme.repository;

import com.subsidytracker.common.entity.SchemeSlab;
import com.subsidytracker.common.enums.BeneficiaryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeSlabRepository extends JpaRepository<SchemeSlab, Long> {
    List<SchemeSlab> findBySchemeId(Long schemeId);
    Optional<SchemeSlab> findBySchemeIdAndCategory(Long schemeId, BeneficiaryCategory category);
}