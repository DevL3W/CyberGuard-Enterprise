package com.cyberguard.dto;

import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.Incident;
import com.cyberguard.entity.IncidentStatus;
import java.time.Instant;

public record IncidentDto(Long id, String title, String description, AlertSeverity severity, IncidentStatus status,
                          Long deviceId, String deviceName, Long assigneeId, String assigneeName,
                          String createdByName, Instant createdAt, Instant closedAt) {
    public static IncidentDto from(Incident i) {
        var d = i.getDevice();
        var a = i.getAssignee();
        var c = i.getCreatedBy();
        return new IncidentDto(i.getId(), i.getTitle(), i.getDescription(), i.getSeverity(), i.getStatus(),
                d == null ? null : d.getId(), d == null ? null : d.getName(),
                a == null ? null : a.getId(), a == null ? null : a.getFullName(),
                c == null ? null : c.getFullName(), i.getCreatedAt(), i.getClosedAt());
    }
}
