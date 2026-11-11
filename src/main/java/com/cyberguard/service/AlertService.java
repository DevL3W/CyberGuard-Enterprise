package com.cyberguard.service;

import com.cyberguard.dto.AlertDto;
import com.cyberguard.dto.AlertRequest;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AppUserDetails;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertService {
    private final AlertRepository alerts;
    private final DeviceRepository devices;
    private final CompanyRepository companies;
    private final AuditService audit;

    public AlertService(AlertRepository alerts, DeviceRepository devices, CompanyRepository companies,
                        AuditService audit) {
        this.alerts = alerts; this.devices = devices; this.companies = companies; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AlertDto> list(AppUserDetails actor) {
        return alerts.findByCompanyIdOrderByCreatedAtDesc(actor.getCompanyId()).stream().map(AlertDto::from).toList();
    }

    @Transactional
    public AlertDto create(AppUserDetails actor, AlertRequest req) {
        Device device = null;
        if (req.deviceId() != null) {
            // Solo se aceptan dispositivos de la misma empresa
            device = devices.findByIdAndCompanyId(req.deviceId(), actor.getCompanyId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El dispositivo elegido no es válido"));
        }
        String description = req.description() == null || req.description().isBlank() ? null : req.description().trim();
        SecurityAlert alert = alerts.save(new SecurityAlert(companies.getReferenceById(actor.getCompanyId()), device,
                req.title().trim(), description, req.severity()));
        audit.record(actor, "CREAR", "Alerta", alert.getId(), alert.getTitle());
        return AlertDto.from(alert);
    }

    @Transactional
    public AlertDto changeStatus(AppUserDetails actor, Long id, AlertStatus status) {
        SecurityAlert alert = alerts.findByIdAndCompanyId(id, actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta no encontrada"));
        alert.setStatus(status);
        alert.setResolvedAt(status == AlertStatus.RESUELTA ? Instant.now() : null);
        audit.record(actor, "MODIFICAR", "Alerta", alert.getId(), alert.getTitle() + " → " + status);
        return AlertDto.from(alert);
    }
}
