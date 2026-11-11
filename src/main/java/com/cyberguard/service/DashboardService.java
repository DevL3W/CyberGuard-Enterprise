package com.cyberguard.service;

import com.cyberguard.dto.AlertDto;
import com.cyberguard.dto.DashboardSummary;
import com.cyberguard.dto.DashboardSummary.DayCount;
import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.IncidentStatus;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.entity.VulnStatus;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.repository.IncidentRepository;
import com.cyberguard.repository.VulnerabilityRepository;
import com.cyberguard.security.AppUserDetails;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final DeviceRepository devices;
    private final AlertRepository alerts;
    private final VulnerabilityRepository vulns;
    private final IncidentRepository incidents;

    public DashboardService(DeviceRepository devices, AlertRepository alerts, VulnerabilityRepository vulns,
                            IncidentRepository incidents) {
        this.devices = devices; this.alerts = alerts; this.vulns = vulns; this.incidents = incidents;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(AppUserDetails actor) {
        Long cid = actor.getCompanyId();

        Map<String, Long> openBySeverity = new LinkedHashMap<>();
        for (AlertSeverity s : AlertSeverity.values()) {
            openBySeverity.put(s.name(), alerts.countByCompanyIdAndSeverityAndStatusNot(cid, s, AlertStatus.RESUELTA));
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<SecurityAlert> recentWeek = alerts.findByCompanyIdAndCreatedAtAfter(cid,
                today.minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC));
        Map<LocalDate, Long> perDay = recentWeek.stream().collect(Collectors.groupingBy(
                a -> a.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), Collectors.counting()));
        List<DayCount> last7 = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            last7.add(new DayCount(day.toString(), perDay.getOrDefault(day, 0L)));
        }

        return new DashboardSummary(
                devices.countByCompanyId(cid),
                devices.countByCompanyIdAndStatus(cid, DeviceStatus.ONLINE),
                devices.countByCompanyIdAndStatus(cid, DeviceStatus.OFFLINE),
                devices.countByCompanyIdAndStatus(cid, DeviceStatus.MANTENIMIENTO),
                alerts.countByCompanyIdAndStatusNot(cid, AlertStatus.RESUELTA),
                openBySeverity.get(AlertSeverity.CRITICA.name()),
                vulns.countByCompanyIdAndStatusNot(cid, VulnStatus.CORREGIDA),
                incidents.countByCompanyIdAndStatusNot(cid, IncidentStatus.CERRADO),
                openBySeverity,
                last7,
                alerts.findTop5ByCompanyIdOrderByCreatedAtDesc(cid).stream().map(AlertDto::from).toList());
    }
}
