package com.cyberguard.dto;

import com.cyberguard.entity.AuditLog;
import java.time.Instant;

public record AuditDto(Long id, String username, String action, String entityType, Long entityId,
                       String detail, Instant createdAt) {
    public static AuditDto from(AuditLog l) {
        return new AuditDto(l.getId(), l.getUsername(), l.getAction(), l.getEntityType(), l.getEntityId(),
                l.getDetail(), l.getCreatedAt());
    }
}
