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
    List<Application> findByStatusIn(List<ApplicationStatus> statuses);
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);
    
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a WHERE a.status IN :statuses AND LOWER(a.beneficiary.region) = LOWER(:region)")
    List<Application> findByStatusInAndRegion(@org.springframework.data.repository.query.Param("statuses") List<ApplicationStatus> statuses, @org.springframework.data.repository.query.Param("region") String region);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a WHERE a.status = :status AND LOWER(a.beneficiary.region) = LOWER(:region)")
    List<Application> findByStatusAndRegion(@org.springframework.data.repository.query.Param("status") ApplicationStatus status, @org.springframework.data.repository.query.Param("region") String region);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a WHERE a.status = :status AND LOWER(a.beneficiary.region) = LOWER(:region)")
    Page<Application> findByStatusAndRegion(@org.springframework.data.repository.query.Param("status") ApplicationStatus status, @org.springframework.data.repository.query.Param("region") String region, Pageable pageable);

    List<Application> findByBeneficiaryId(Long beneficiaryId);
    Page<Application> findByBeneficiaryId(Long beneficiaryId, Pageable pageable);
    boolean existsByBeneficiaryIdAndSchemeIdAndStatusNotIn(Long beneficiaryId, Long schemeId, List<ApplicationStatus> statuses);
}