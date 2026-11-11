package com.cyberguard.service;

import com.cyberguard.dto.AgentKeyDto;
import com.cyberguard.dto.DeviceDto;
import com.cyberguard.dto.DeviceRequest;
import com.cyberguard.entity.Device;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AgentKeys;
import com.cyberguard.security.AppUserDetails;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Inventario de dispositivos, siempre limitado a la empresa del usuario autenticado. */
@Service
public class DeviceService {
    private final DeviceRepository devices;
    private final CompanyRepository companies;
    private final AuditService audit;

    public DeviceService(DeviceRepository devices, CompanyRepository companies, AuditService audit) {
        this.devices = devices; this.companies = companies; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DeviceDto> list(AppUserDetails actor) {
        return devices.findByCompanyIdOrderByNameAsc(actor.getCompanyId()).stream().map(DeviceDto::from).toList();
    }

    @Transactional
    public DeviceDto create(AppUserDetails actor, DeviceRequest req) {
        Device d = new Device(companies.getReferenceById(actor.getCompanyId()));
        apply(d, req);
        Device saved = devices.save(d);
        audit.record(actor, "CREAR", "Dispositivo", saved.getId(), saved.getName());
        return DeviceDto.from(saved);
    }

    @Transactional
    public DeviceDto update(AppUserDetails actor, Long id, DeviceRequest req) {
        Device d = find(actor, id);
        apply(d, req);
        audit.record(actor, "MODIFICAR", "Dispositivo", d.getId(), d.getName());
        return DeviceDto.from(d);
    }

    @Transactional
    public void delete(AppUserDetails actor, Long id) {
        Device d = find(actor, id);
        String name = d.getName();
        devices.delete(d);
        audit.record(actor, "ELIMINAR", "Dispositivo", id, name);
    }

    /** Genera una clave nueva para el agente del dispositivo. La anterior deja de funcionar. */
    @Transactional
    public AgentKeyDto generateAgentKey(AppUserDetails actor, Long id) {
        Device d = find(actor, id);
        String key = AgentKeys.generate();
        d.setAgentKeyHash(AgentKeys.hash(key));
        audit.record(actor, "CLAVE_AGENTE", "Dispositivo", d.getId(), "Nueva clave de agente para " + d.getName());
        return new AgentKeyDto(d.getId(), key);
    }

    private Device find(AppUserDetails actor, Long id) {
        return devices.findByIdAndCompanyId(id, actor.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo no encontrado"));
    }

    private void apply(Device d, DeviceRequest r) {
        d.setName(r.name().trim());
        d.setHostname(blankToNull(r.hostname()));
        d.setIpAddress(blankToNull(r.ipAddress()));
        d.setType(r.type());
        d.setOperatingSystem(blankToNull(r.operatingSystem()));
        d.setStatus(r.status());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
