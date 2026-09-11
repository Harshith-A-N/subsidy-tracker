package com.subsidytracker.common.controller;

import com.subsidytracker.common.dto.AuditLogResponseDto;
import com.subsidytracker.common.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Get recent audit logs with optional filtering by entity, entityId, and action.
     */
    @GetMapping
    public ResponseEntity<List<AuditLogResponseDto>> getAuditLogs(
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String action) {
        return ResponseEntity.ok(auditLogService.getRecentAuditLogsDto(entityName, entityId, action));
    }

    /**
     * Get audit logs for a specific entity (e.g. Application, Document, User).
     */
    @GetMapping("/entity/{entityName}/{entityId}")
    public ResponseEntity<List<AuditLogResponseDto>> getEntityAuditLogs(
            @PathVariable String entityName,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(auditLogService.getRecentAuditLogsDto(entityName, entityId, null));
    }

    /**
     * Get complete lifecycle audit trail for an application (status transitions, officer decisions, document checks).
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<AuditLogResponseDto>> getApplicationAuditLogs(
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(auditLogService.getAuditLogsForApplicationDto(applicationId));
    }
}
