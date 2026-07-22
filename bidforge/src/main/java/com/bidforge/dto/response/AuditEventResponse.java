package com.bidforge.dto.response;

import com.bidforge.entity.enums.AuditAction;

import java.time.Instant;

// One audit log line as shown to administrators
public record AuditEventResponse(
        Long id,
        String actor,
        AuditAction action,
        String entityType,
        Long entityId,
        String details,
        Instant createdAt
) {
}
