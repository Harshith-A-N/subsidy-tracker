package com.subsidytracker.eligibility.repository;

import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRoleAndRegion(Role role, String region);
    Optional<User> findByEmail(String email);
}