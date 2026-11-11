package com.cyberguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alerts")
public class SecurityAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id")
    private Device device;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AlertStatus status = AlertStatus.ABIERTA;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected SecurityAlert() {}
    public SecurityAlert(Company company, Device device, String title, String description, AlertSeverity severity) {
        this.company = company; this.device = device; this.title = title;
        this.description = description; this.severity = severity;
    }

    public Long getId() { return id; }
    public Device getDevice() { return device; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
