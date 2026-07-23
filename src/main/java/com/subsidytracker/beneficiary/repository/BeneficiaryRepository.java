package com.subsidytracker.beneficiary.repository;

import com.subsidytracker.common.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    // Used to check for duplicate registration before creating a new Beneficiary
    Optional<Beneficiary> findByNationalIdNumber(String nationalIdNumber);

    // Used by Module 4 analytics: beneficiary count/breakdown by category
    List<Beneficiary> findByCategory(com.subsidytracker.common.enums.BeneficiaryCategory category);

    // Used by routing logic: find all beneficiaries in a given region
    List<Beneficiary> findByRegion(String region);
}