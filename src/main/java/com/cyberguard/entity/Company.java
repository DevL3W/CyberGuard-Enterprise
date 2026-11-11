package com.cyberguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "companies")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "license_key", nullable = false, unique = true, length = 64)
    private String licenseKey;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Company() {}
    public Company(String name, String licenseKey) { this.name = name; this.licenseKey = licenseKey; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLicenseKey() { return licenseKey; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
