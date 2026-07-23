package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.Verification;
import com.subsidytracker.common.enums.VerificationLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {
    List<Verification> findByApplicationIdOrderByVerificationDateAsc(Long applicationId);
    boolean existsByApplicationIdAndLevel(Long applicationId, VerificationLevel level);
    Optional<Verification> findTopByApplicationIdOrderByVerificationDateDesc(Long applicationId);
}