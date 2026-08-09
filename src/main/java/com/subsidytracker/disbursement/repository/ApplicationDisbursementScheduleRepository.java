package com.subsidytracker.disbursement.repository;

import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationDisbursementScheduleRepository extends JpaRepository<ApplicationDisbursementSchedule, Long> {
    List<ApplicationDisbursementSchedule> findByApplicationIdOrderByStageSequenceNumberAsc(Long applicationId);

    boolean existsByApplicationId(Long applicationId);

    List<ApplicationDisbursementSchedule> findByApplicationId(Long applicationId);
}
