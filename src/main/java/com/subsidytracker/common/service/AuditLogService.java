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

    /**
     * Retrieves recent audit logs as DTOs with optional filtering by entity, entityId, and action.
     */
    public List<com.subsidytracker.common.dto.AuditLogResponseDto> getRecentAuditLogsDto(
            String entityName, Long entityId, String action) {
        List<AuditLog> logs;

        if (entityName != null && !entityName.isBlank() && entityId != null) {
            logs = auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(entityName, entityId);
            if (logs.isEmpty()) {
                // Try case variations (e.g., Application vs APPLICATION)
                String altEntity = Character.toUpperCase(entityName.charAt(0)) + entityName.substring(1).toLowerCase();
                logs = auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(altEntity, entityId);
            }
        } else if (entityName != null && !entityName.isBlank()) {
            logs = auditLogRepository.findByEntityNameOrderByTimestampDesc(entityName);
            if (logs.isEmpty()) {
                String altEntity = Character.toUpperCase(entityName.charAt(0)) + entityName.substring(1).toLowerCase();
                logs = auditLogRepository.findByEntityNameOrderByTimestampDesc(altEntity);
            }
        } else {
            logs = auditLogRepository.findTop200ByOrderByTimestampDesc();
        }

        if (action != null && !action.isBlank()) {
            logs = logs.stream()
                    .filter(l -> l.getAction() != null && l.getAction().equalsIgnoreCase(action.trim()))
                    .toList();
        }

        return logs.stream().map(this::mapToDto).toList();
    }

    /**
     * Retrieves all audit logs relevant to a given application (application lifecycle, document decisions, schedules).
     */
    public List<com.subsidytracker.common.dto.AuditLogResponseDto> getAuditLogsForApplicationDto(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }

        List<AuditLog> allLogs = auditLogRepository.findAllByOrderByTimestampDesc();
        String appIdPattern1 = "application id: " + applicationId;
        String appIdPattern2 = "application #" + applicationId;
        String appIdPattern3 = "application id=" + applicationId;

        return allLogs.stream()
                .filter(l -> (("Application".equalsIgnoreCase(l.getEntityName())) && applicationId.equals(l.getEntityId()))
                        || (l.getDetails() != null && (
                                l.getDetails().contains(appIdPattern1) ||
                                l.getDetails().contains(appIdPattern2) ||
                                l.getDetails().contains(appIdPattern3)
                        )))
                .map(this::mapToDto)
                .toList();
    }

    public com.subsidytracker.common.dto.AuditLogResponseDto mapToDto(AuditLog log) {
        com.subsidytracker.common.dto.AuditLogResponseDto.AuditLogResponseDtoBuilder builder =
                com.subsidytracker.common.dto.AuditLogResponseDto.builder()
                        .id(log.getId())
                        .entityName(log.getEntityName())
                        .entityId(log.getEntityId())
                        .action(log.getAction())
                        .timestamp(log.getTimestamp())
                        .details(log.getDetails());

        if (log.getActor() != null) {
            builder.actorId(log.getActor().getId())
                    .actorName(log.getActor().getFullName())
                    .actorEmail(log.getActor().getEmail())
                    .actorRole(log.getActor().getRole() != null ? log.getActor().getRole().name() : null);
        } else {
            builder.actorName("System / Automated Engine")
                    .actorRole("SYSTEM");
        }

        return builder.build();
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
