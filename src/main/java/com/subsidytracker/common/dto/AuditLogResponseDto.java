package com.subsidytracker.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDto {
    private Long id;
    private String entityName;
    private Long entityId;
    private String action;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private String actorRole;
    private LocalDateTime timestamp;
    private String details;
}
