package com.cyberguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;
    @Column(name = "user_id", updatable = false)
    private Long userId;
    @Column(nullable = false, updatable = false, length = 60)
    private String username;
    @Column(nullable = false, updatable = false, length = 30)
    private String action;
    @Column(name = "entity_type", nullable = false, updatable = false, length = 30)
    private String entityType;
    @Column(name = "entity_id", updatable = false)
    private Long entityId;
    @Column(updatable = false, length = 500)
    private String detail;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {}
    public AuditLog(Long companyId, Long userId, String username, String action, String entityType,
                    Long entityId, String detail) {
        this.companyId = companyId; this.userId = userId; this.username = username; this.action = action;
        this.entityType = entityType; this.entityId = entityId; this.detail = detail;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
