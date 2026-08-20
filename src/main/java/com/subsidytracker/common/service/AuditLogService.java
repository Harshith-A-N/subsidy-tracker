package com.subsidytracker.common.service;

import com.subsidytracker.common.entity.AuditLog;
import com.subsidytracker.common.entity.User;
import com.subsidytracker.common.repository.AuditLogRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reusable service for recording audit logs on critical state-changing operations.
 * Resolves authenticated user from SecurityContext when actor is not explicitly provided,
 * while allowing explicit User or actorUserId for service-to-service/system calls.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Log an audit event with an optional explicit User object.
     * If actor is null, attempts to resolve the current authenticated user from SecurityContext.
     */
    @Transactional
    public AuditLog logEvent(String entityName, Long entityId, String action, User actor, String details) {
        if (actor == null) {
            actor = resolveCurrentActorFromSecurityContext();
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setActor(actor);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(details);

        return auditLogRepository.save(auditLog);
    }

    /**
     * Log an audit event using an explicit user ID.
     */
    @Transactional
    public AuditLog logEvent(String entityName, Long entityId, String action, Long actorUserId, String details) {
        User actor = null;
        if (actorUserId != null) {
            actor = userRepository.findById(actorUserId).orElse(null);
        }
        return logEvent(entityName, entityId, action, actor, details);
    }

    /**
     * Log a system audit event where no user actor is associated.
     */
    @Transactional
    public AuditLog logSystemEvent(String entityName, Long entityId, String action, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setActor(null);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(details);

        return auditLogRepository.save(auditLog);
    }

    /**
     * Retrieves audit log history for a specific entity.
     */
    public List<AuditLog> getAuditLogsForEntity(String entityName, Long entityId) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId);
    }

    private User resolveCurrentActorFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }
}
