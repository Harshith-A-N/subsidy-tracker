package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for User entities.
 *
 * Used by Module 2 to validate that the officer performing a verification
 * actually exists and has the correct role for the verification level.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find all users with a specific role (e.g., all FIELD_OFFICERs)
    List<User> findByRole(Role role);
}
