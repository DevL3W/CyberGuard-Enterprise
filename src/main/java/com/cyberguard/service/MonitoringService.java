package com.cyberguard.service;

import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.DeviceRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Detecta dispositivos con agente que dejaron de enviar señal y genera la alerta de desconexión. */
@Service
public class MonitoringService {
    private final DeviceRepository devices;
    private final AlertRepository alerts;
    private final AuditService audit;
    private final long offlineAfterSeconds;

    public MonitoringService(DeviceRepository devices, AlertRepository alerts, AuditService audit,
                             @Value("${cyberguard.monitoring.offline-after-seconds:180}") long offlineAfterSeconds) {
        this.devices = devices; this.alerts = alerts; this.audit = audit;
        this.offlineAfterSeconds = offlineAfterSeconds;
    }

    @Scheduled(fixedDelayString = "${cyberguard.monitoring.check-interval-ms:30000}",
               initialDelayString = "${cyberguard.monitoring.check-interval-ms:30000}")
    @Transactional
    public void checkDisconnections() {
        Instant limit = Instant.now().minusSeconds(offlineAfterSeconds);
        // Solo los dispositivos que alguna vez enviaron señal (lastSeen no nulo) pueden pasar a sin conexión
        for (Device d : devices.findByStatusAndLastSeenBefore(DeviceStatus.ONLINE, limit)) {
            d.setStatus(DeviceStatus.OFFLINE);
            alerts.save(new SecurityAlert(d.getCompany(), d, AgentService.DISCONNECT_TITLE,
                    "No se recibió señal del agente en los últimos " + offlineAfterSeconds + " segundos.",
                    AlertSeverity.ALTA));
            audit.record(d.getCompany().getId(), null, "sistema", "DESCONEXION", "Dispositivo", d.getId(), d.getName());
        }
    }
}
