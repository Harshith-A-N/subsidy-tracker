package com.subsidytracker.disbursement.repository;

import com.subsidytracker.disbursement.entity.DisbursementStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisbursementStageRepository extends JpaRepository<DisbursementStage, Long> {
    List<DisbursementStage> findByPlanIdOrderBySequenceNumberAsc(Long planId);
}
