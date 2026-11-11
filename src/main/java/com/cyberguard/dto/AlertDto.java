package com.cyberguard.dto;

import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.SecurityAlert;
import java.time.Instant;

public record AlertDto(Long id, String title, String description, AlertSeverity severity, AlertStatus status,
                       Long deviceId, String deviceName, Instant createdAt, Instant resolvedAt) {
    public static AlertDto from(SecurityAlert a) {
        var d = a.getDevice();
        return new AlertDto(a.getId(), a.getTitle(), a.getDescription(), a.getSeverity(), a.getStatus(),
                d == null ? null : d.getId(), d == null ? null : d.getName(), a.getCreatedAt(), a.getResolvedAt());
    }
}
