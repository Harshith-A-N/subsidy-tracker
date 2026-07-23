package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStatus(ApplicationStatus status);
    List<Application> findByBeneficiaryId(Long beneficiaryId);
}