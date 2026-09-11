package com.subsidytracker.common.repository;

import com.subsidytracker.common.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityNameAndEntityId(String entityName, Long entityId);
    List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, Long entityId);
    List<AuditLog> findAllByOrderByTimestampDesc();
    List<AuditLog> findTop200ByOrderByTimestampDesc();
    List<AuditLog> findByEntityNameOrderByTimestampDesc(String entityName);
}

