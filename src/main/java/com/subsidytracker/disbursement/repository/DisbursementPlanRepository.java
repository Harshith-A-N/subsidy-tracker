package com.subsidytracker.disbursement.repository;

import com.subsidytracker.disbursement.entity.DisbursementPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisbursementPlanRepository extends JpaRepository<DisbursementPlan, Long> {
    Optional<DisbursementPlan> findBySchemeId(Long schemeId);
}
