package com.cyberguard.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 150)
    private String hostname;
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceType type;
    @Column(name = "operating_system", length = 100)
    private String operatingSystem;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status;
    @Column(name = "last_seen")
    private Instant lastSeen;
    @Column(name = "agent_key_hash", length = 64)
    private String agentKeyHash;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Device() {}
    public Device(Company company) { this.company = company; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public DeviceType getType() { return type; }
    public void setType(DeviceType type) { this.type = type; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
    public String getAgentKeyHash() { return agentKeyHash; }
    public void setAgentKeyHash(String agentKeyHash) { this.agentKeyHash = agentKeyHash; }
    public Company getCompany() { return company; }
    public Instant getCreatedAt() { return createdAt; }
}
