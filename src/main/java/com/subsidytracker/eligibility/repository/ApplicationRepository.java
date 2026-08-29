package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.Application;
import com.subsidytracker.common.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStatus(ApplicationStatus status);
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);
    List<Application> findByBeneficiaryId(Long beneficiaryId);
    Page<Application> findByBeneficiaryId(Long beneficiaryId, Pageable pageable);
}