package com.example.taskmanagement.dto.audit;

import com.example.taskmanagement.entity.AuditAction;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actor,
        AuditAction action,
        String targetType,
        String targetId,
        String details,
        Instant createdAt
) {
}
