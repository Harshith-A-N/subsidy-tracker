package com.subsidytracker.security.repository;

import com.subsidytracker.common.enums.RequestStatus;
import com.subsidytracker.security.entity.OfficerRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerRegistrationRequestRepository extends JpaRepository<OfficerRegistrationRequest, Long> {

    Optional<OfficerRegistrationRequest> findByEmailAndStatus(String email, RequestStatus status);

    boolean existsByEmailAndStatus(String email, RequestStatus status);

    List<OfficerRegistrationRequest> findByStatusOrderBySubmittedAtDesc(RequestStatus status);

    List<OfficerRegistrationRequest> findAllByOrderBySubmittedAtDesc();
}
