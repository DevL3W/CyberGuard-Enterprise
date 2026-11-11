package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.DeviceRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MonitoringServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final AlertRepository alerts = mock(AlertRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final MonitoringService service = new MonitoringService(devices, alerts, audit, 180);

    @Test
    void dispositivoSinSenalPasaASinConexionYGeneraAlerta() {
        Device d = new Device(new Company("Acme", "K-1"));
        d.setName("Servidor");
        d.setStatus(DeviceStatus.ONLINE);
        d.setLastSeen(Instant.now().minusSeconds(600));
        when(devices.findByStatusAndLastSeenBefore(eq(DeviceStatus.ONLINE), any(Instant.class))).thenReturn(List.of(d));

        service.checkDisconnections();

        assertEquals(DeviceStatus.OFFLINE, d.getStatus());
        verify(alerts).save(argThat((SecurityAlert a) -> a.getTitle().equals(AgentService.DISCONNECT_TITLE)
                && a.getSeverity() == AlertSeverity.ALTA && a.getDevice() == d));
    }

    @Test
    void sinDispositivosVencidosNoHaceNada() {
        when(devices.findByStatusAndLastSeenBefore(eq(DeviceStatus.ONLINE), any(Instant.class))).thenReturn(List.of());
        service.checkDisconnections();
        verify(alerts, never()).save(any());
    }
}
