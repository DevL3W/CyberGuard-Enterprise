package com.cyberguard.service;

import com.cyberguard.dto.EventRequest;
import com.cyberguard.dto.HeartbeatRequest;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AgentKeys;
import com.cyberguard.security.AgentPrincipal;
import com.cyberguard.security.AgentRateLimiter;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Recepción de señales y eventos de los agentes instalados en los dispositivos autorizados. */
@Service
public class AgentService {
    public static final String DISCONNECT_TITLE = "Dispositivo sin conexión";

    private final DeviceRepository devices;
    private final AlertRepository alerts;
    private final AuditService audit;
    private final AgentRateLimiter limiter;

    public AgentService(DeviceRepository devices, AlertRepository alerts, AuditService audit, AgentRateLimiter limiter) {
        this.devices = devices; this.alerts = alerts; this.audit = audit; this.limiter = limiter;
    }

    /** Valida el ID de dispositivo y la clave del agente; la empresa debe tener la licencia activa. */
    @Transactional(readOnly = true)
    public Optional<AgentPrincipal> authenticate(Long deviceId, String key) {
        return devices.findById(deviceId)
                .filter(d -> AgentKeys.matches(key, d.getAgentKeyHash()))
                .filter(d -> d.getCompany().isActive())
                .map(d -> new AgentPrincipal(d.getId(), d.getCompany().getId(), d.getName()));
    }

    @Transactional
    public void heartbeat(AgentPrincipal principal, HeartbeatRequest req) {
        Device d = devices.findById(principal.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo no encontrado"));
        d.setLastSeen(Instant.now());
        if (notBlank(req.hostname())) d.setHostname(req.hostname().trim());
        if (notBlank(req.ipAddress())) d.setIpAddress(req.ipAddress().trim());
        if (notBlank(req.operatingSystem())) d.setOperatingSystem(req.operatingSystem().trim());

        if (d.getStatus() != DeviceStatus.MANTENIMIENTO) {
            if (d.getStatus() == DeviceStatus.OFFLINE) {
                // El dispositivo volvió: se cierran las alertas de desconexión que siguieran abiertas
                for (SecurityAlert a : alerts.findByDeviceIdAndTitleAndStatusNot(d.getId(), DISCONNECT_TITLE, AlertStatus.RESUELTA)) {
                    a.setStatus(AlertStatus.RESUELTA);
                    a.setResolvedAt(Instant.now());
                }
            }
            d.setStatus(DeviceStatus.ONLINE);
        }
    }

    @Transactional
    public void event(AgentPrincipal principal, EventRequest req) {
        if (!limiter.allow("device-" + principal.deviceId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados eventos. Reduzca la frecuencia");
        }
        Device d = devices.findById(principal.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo no encontrado"));
        String description = notBlank(req.description()) ? req.description().trim() : null;
        SecurityAlert alert = alerts.save(new SecurityAlert(d.getCompany(), d, req.title().trim(), description, req.severity()));
        audit.record(principal.companyId(), null, "agente:" + d.getName(), "CREAR", "Alerta", alert.getId(), alert.getTitle());
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
