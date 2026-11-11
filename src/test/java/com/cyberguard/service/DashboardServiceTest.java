package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.Role;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.repository.IncidentRepository;
import com.cyberguard.repository.VulnerabilityRepository;
import com.cyberguard.security.AppUserDetails;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final AlertRepository alerts = mock(AlertRepository.class);
    private final VulnerabilityRepository vulns = mock(VulnerabilityRepository.class);
    private final IncidentRepository incidents = mock(IncidentRepository.class);
    private final DashboardService service = new DashboardService(devices, alerts, vulns, incidents);
    private final AppUserDetails actor = new AppUserDetails(1L, 10L, "Acme", "ana", "h", "Ana", Role.LECTOR, true);

    @Test
    void resumenCuentaDispositivosYAlertas() {
        SecurityAlert hoy = new SecurityAlert(new Company("Acme", "K"), null, "Hoy", null, AlertSeverity.CRITICA);
        when(devices.countByCompanyId(10L)).thenReturn(5L);
        when(devices.countByCompanyIdAndStatus(10L, DeviceStatus.ONLINE)).thenReturn(3L);
        when(devices.countByCompanyIdAndStatus(10L, DeviceStatus.OFFLINE)).thenReturn(1L);
        when(devices.countByCompanyIdAndStatus(10L, DeviceStatus.MANTENIMIENTO)).thenReturn(1L);
        when(alerts.countByCompanyIdAndStatusNot(10L, AlertStatus.RESUELTA)).thenReturn(4L);
        when(alerts.countByCompanyIdAndSeverityAndStatusNot(eq(10L), any(AlertSeverity.class), eq(AlertStatus.RESUELTA)))
                .thenReturn(0L);
        when(alerts.countByCompanyIdAndSeverityAndStatusNot(10L, AlertSeverity.CRITICA, AlertStatus.RESUELTA))
                .thenReturn(2L);
        when(alerts.findByCompanyIdAndCreatedAtAfter(eq(10L), any())).thenReturn(List.of(hoy));
        when(alerts.findTop5ByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(hoy));

        when(vulns.countByCompanyIdAndStatusNot(10L, com.cyberguard.entity.VulnStatus.CORREGIDA)).thenReturn(6L);
        when(incidents.countByCompanyIdAndStatusNot(10L, com.cyberguard.entity.IncidentStatus.CERRADO)).thenReturn(2L);

        var s = service.summary(actor);

        assertEquals(5, s.totalDevices());
        assertEquals(3, s.devicesOnline());
        assertEquals(4, s.openAlerts());
        assertEquals(2, s.criticalOpen());
        assertEquals(6, s.openVulnerabilities());
        assertEquals(2, s.openIncidents());
        assertEquals(4, s.openBySeverity().size());
        assertEquals(7, s.alertsLast7Days().size());
        assertEquals(1, s.alertsLast7Days().get(6).count());
        assertEquals(1, s.recentAlerts().size());
    }
}
