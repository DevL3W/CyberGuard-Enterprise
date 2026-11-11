package com.cyberguard.dto;

import java.util.List;
import java.util.Map;

public record DashboardSummary(long totalDevices, long devicesOnline, long devicesOffline, long devicesMaintenance,
                               long openAlerts, long criticalOpen,
                               long openVulnerabilities, long openIncidents, Map<String, Long> openBySeverity,
                               List<DayCount> alertsLast7Days, List<AlertDto> recentAlerts) {
    /** Número de alertas creadas en un día (UTC), con fecha en formato AAAA-MM-DD. */
    public record DayCount(String date, long count) {}
}
