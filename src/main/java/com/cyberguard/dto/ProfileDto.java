package com.cyberguard.dto;

import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.IncidentStatus;
import com.cyberguard.entity.Role;
import java.time.Instant;
import java.util.List;

public record ProfileDto(Long id, String username, String fullName, String email, String jobTitle, Role role,
                         String companyName, Instant createdAt, List<IncidentSummary> assignedIncidents,
                         List<AuditDto> recentActivity) {
    public record IncidentSummary(Long id, String title, AlertSeverity severity, IncidentStatus status) {}
}
