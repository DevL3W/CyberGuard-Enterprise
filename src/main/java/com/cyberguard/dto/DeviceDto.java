package com.cyberguard.dto;

import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.DeviceType;
import java.time.Instant;

public record DeviceDto(Long id, String name, String hostname, String ipAddress, DeviceType type,
                        String operatingSystem, DeviceStatus status, Instant lastSeen, Instant createdAt,
                        boolean hasAgent) {
    public static DeviceDto from(Device d) {
        return new DeviceDto(d.getId(), d.getName(), d.getHostname(), d.getIpAddress(), d.getType(),
                d.getOperatingSystem(), d.getStatus(), d.getLastSeen(), d.getCreatedAt(),
                d.getAgentKeyHash() != null);
    }
}
