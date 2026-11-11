package com.cyberguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id")
    private Device device;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", updatable = false)
    private AppUser createdBy;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.ABIERTO;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "closed_at")
    private Instant closedAt;

    protected Incident() {}
    public Incident(Company company, Device device, AppUser assignee, AppUser createdBy, String title,
                    String description, AlertSeverity severity) {
        this.company = company; this.device = device; this.assignee = assignee; this.createdBy = createdBy;
        this.title = title; this.description = description; this.severity = severity;
    }

    public Long getId() { return id; }
    public Device getDevice() { return device; }
    public AppUser getAssignee() { return assignee; }
    public void setAssignee(AppUser assignee) { this.assignee = assignee; }
    public AppUser getCreatedBy() { return createdBy; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AlertSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
