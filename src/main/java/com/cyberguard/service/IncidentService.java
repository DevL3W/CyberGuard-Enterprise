package com.cyberguard.service;

import com.cyberguard.dto.IncidentDto;
import com.cyberguard.dto.IncidentRequest;
import com.cyberguard.dto.IncidentUpdateRequest;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.Incident;
import com.cyberguard.entity.IncidentStatus;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.repository.IncidentRepository;
import com.cyberguard.security.AppUserDetails;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IncidentService {
    private final IncidentRepository incidents;
    private final DeviceRepository devices;
    private final AppUserRepository users;
    private final CompanyRepository companies;
    private final AuditService audit;

    public IncidentService(IncidentRepository incidents, DeviceRepository devices, AppUserRepository users,
                           CompanyRepository companies, AuditService audit) {
        this.incidents = incidents; this.devices = devices; this.users = users;
        this.companies = companies; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> list(AppUserDetails actor) {
        return incidents.findByCompanyIdOrderByCreatedAtDesc(actor.getCompanyId()).stream().map(IncidentDto::from).toList();
    }

    @Transactional
    public IncidentDto create(AppUserDetails actor, IncidentRequest req) {
        Device device = null;
        if (req.deviceId() != null) {
            device = devices.findByIdAndCompanyId(req.deviceId(), actor.getCompanyId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El dispositivo elegido no es válido"));
        }
        AppUser assignee = req.assigneeId() == null ? null : findAssignee(actor, req.assigneeId());
        String description = req.description() == null || req.description().isBlank() ? null : req.description().trim();
        Incident i = incidents.save(new Incident(companies.getReferenceById(actor.getCompanyId()), device, assignee,
                users.getReferenceById(actor.getId()), req.title().trim(), description, req.severity()));
        audit.record(actor, "CREAR", "Incidente", i.getId(), i.getTitle());
        return IncidentDto.from(i);
    }

    @Transactional
    public IncidentDto update(AppUserDetails actor, Long id, IncidentUpdateRequest req) {
        Incident i = incidents.findByIdAndCompanyId(id, actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado"));
        if (req.status() != null) {
            i.setStatus(req.status());
            i.setClosedAt(req.status() == IncidentStatus.CERRADO ? Instant.now() : null);
        }
        if (req.assigneeId() != null) {
            i.setAssignee(findAssignee(actor, req.assigneeId()));
        }
        audit.record(actor, "MODIFICAR", "Incidente", i.getId(), i.getTitle() + " → " + i.getStatus()
                + (i.getAssignee() == null ? "" : ", responsable " + i.getAssignee().getFullName()));
        return IncidentDto.from(i);
    }

    /** El responsable debe ser una sesión activa de la misma empresa. */
    private AppUser findAssignee(AppUserDetails actor, Long assigneeId) {
        return users.findByIdAndCompanyId(assigneeId, actor.getCompanyId()).filter(AppUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El responsable elegido no es válido"));
    }
}
