package com.subsidytracker.scheme.repository;

import com.subsidytracker.common.entity.RegionalBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionalBudgetRepository extends JpaRepository<RegionalBudget, Long> {
    List<RegionalBudget> findBySchemeId(Long schemeId);
    Optional<RegionalBudget> findBySchemeIdAndRegionName(Long schemeId, String regionName);
}