package com.subsidytracker.disbursement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ApplicationDisbursementScheduleRepository extends JpaRepository<ApplicationDisbursementSchedule, Long> {
    List<ApplicationDisbursementSchedule> findByApplicationIdOrderByStageSequenceNumberAsc(Long applicationId);
    Page<ApplicationDisbursementSchedule> findByApplicationIdOrderByStageSequenceNumberAsc(Long applicationId, Pageable pageable);

    boolean existsByApplicationId(Long applicationId);

    List<ApplicationDisbursementSchedule> findByApplicationId(Long applicationId);

    // Used to block edits/deletes of a DisbursementPlan once any application
    // already has a generated schedule against one of its stages — deleting
    // a stage that a schedule/milestone still points to (both are non-nullable
    // FKs) would otherwise throw a raw DB integrity violation.
    boolean existsByStageIdIn(List<Long> stageIds);
}