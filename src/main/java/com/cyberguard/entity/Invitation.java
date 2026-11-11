package com.cyberguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Invitación para unirse a una empresa. Solo se guarda la huella SHA-256 del enlace, nunca el enlace. */
@Entity
@Table(name = "invitations")
public class Invitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private Role role;
    @Column(name = "max_uses", nullable = false, updatable = false)
    private int maxUses;
    @Column(nullable = false)
    private int uses;
    @Column(nullable = false)
    private boolean revoked;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(name = "created_by", updatable = false)
    private Long createdBy;
    @Column(name = "created_by_name", updatable = false, length = 60)
    private String createdByName;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Invitation() {}
    public Invitation(Long companyId, String tokenHash, Role role, int maxUses, Instant expiresAt,
                      Long createdBy, String createdByName) {
        this.companyId = companyId; this.tokenHash = tokenHash; this.role = role; this.maxUses = maxUses;
        this.expiresAt = expiresAt; this.createdBy = createdBy; this.createdByName = createdByName;
    }

    /** Vigente: no revocada, no vencida y con usos disponibles. */
    public boolean isUsable(Instant now) { return !revoked && uses < maxUses && expiresAt.isAfter(now); }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getTokenHash() { return tokenHash; }
    public Role getRole() { return role; }
    public int getMaxUses() { return maxUses; }
    public int getUses() { return uses; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public Instant getExpiresAt() { return expiresAt; }
    public Long getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    public Instant getCreatedAt() { return createdAt; }
}
