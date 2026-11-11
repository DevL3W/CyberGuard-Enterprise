package com.cyberguard.dto;

import java.time.Instant;
import java.util.Map;

public record ReportOverview(Map<String, Long> devicesByType, Map<String, Long> devicesByStatus,
                             Map<String, Long> alertsBySeverity, Map<String, Long> alertsByStatus,
                             Map<String, Long> openVulnerabilitiesBySeverity, Map<String, Long> incidentsByStatus,
                             Double avgAlertResolutionHours, Instant generatedAt) {}
