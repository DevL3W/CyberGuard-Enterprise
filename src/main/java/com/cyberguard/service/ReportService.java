package com.cyberguard.service;

import com.cyberguard.dto.ReportOverview;
import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.DeviceType;
import com.cyberguard.entity.IncidentStatus;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.entity.VulnStatus;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.repository.IncidentRepository;
import com.cyberguard.repository.VulnerabilityRepository;
import com.cyberguard.security.AppUserDetails;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final DeviceRepository devices;
    private final AlertRepository alerts;
    private final VulnerabilityRepository vulns;
    private final IncidentRepository incidents;

    public ReportService(DeviceRepository devices, AlertRepository alerts, VulnerabilityRepository vulns,
                         IncidentRepository incidents) {
        this.devices = devices; this.alerts = alerts; this.vulns = vulns; this.incidents = incidents;
    }

    @Transactional(readOnly = true)
    public ReportOverview overview(AppUserDetails actor) {
        Long cid = actor.getCompanyId();
        var devs = devices.findByCompanyIdOrderByNameAsc(cid);
        var als = alerts.findByCompanyIdOrderByCreatedAtDesc(cid);
        var vs = vulns.findByCompanyIdOrderByCreatedAtDesc(cid).stream()
                .filter(v -> v.getStatus() != VulnStatus.CORREGIDA).toList();
        var incs = incidents.findByCompanyIdOrderByCreatedAtDesc(cid);

        double avg = als.stream().filter(a -> a.getResolvedAt() != null)
                .mapToLong(a -> Duration.between(a.getCreatedAt(), a.getResolvedAt()).toMinutes()).average().orElse(-1);

        return new ReportOverview(
                count(devs, DeviceType.values(), Device::getType),
                count(devs, DeviceStatus.values(), Device::getStatus),
                count(als, AlertSeverity.values(), SecurityAlert::getSeverity),
                count(als, AlertStatus.values(), SecurityAlert::getStatus),
                count(vs, AlertSeverity.values(), v -> v.getSeverity()),
                count(incs, IncidentStatus.values(), i -> i.getStatus()),
                avg < 0 ? null : Math.round(avg / 60.0 * 10.0) / 10.0,
                Instant.now());
    }

    /** CSV del informe pedido (devices, alerts, vulnerabilities o incidents); vacío si el nombre no existe. */
    @Transactional(readOnly = true)
    public Optional<String> csv(String name, AppUserDetails actor) {
        Long cid = actor.getCompanyId();
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM para que Excel respete los acentos
        switch (name) {
            case "devices" -> {
                sb.append(Csv.row("Nombre", "Host", "IP", "Tipo", "Sistema", "Estado", "Última conexión", "Agente"));
                devices.findByCompanyIdOrderByNameAsc(cid).forEach(d -> sb.append(Csv.row(d.getName(), d.getHostname(),
                        d.getIpAddress(), d.getType(), d.getOperatingSystem(), d.getStatus(), d.getLastSeen(),
                        d.getAgentKeyHash() != null ? "Sí" : "No")));
            }
            case "alerts" -> {
                sb.append(Csv.row("Fecha", "Severidad", "Título", "Descripción", "Dispositivo", "Estado", "Resuelta"));
                alerts.findByCompanyIdOrderByCreatedAtDesc(cid).forEach(a -> sb.append(Csv.row(a.getCreatedAt(),
                        a.getSeverity(), a.getTitle(), a.getDescription(),
                        a.getDevice() == null ? null : a.getDevice().getName(), a.getStatus(), a.getResolvedAt())));
            }
            case "vulnerabilities" -> {
                sb.append(Csv.row("Fecha", "Severidad", "Título", "CVE", "Dispositivo", "Estado", "Corregida"));
                vulns.findByCompanyIdOrderByCreatedAtDesc(cid).forEach(v -> sb.append(Csv.row(v.getCreatedAt(),
                        v.getSeverity(), v.getTitle(), v.getCve(),
                        v.getDevice() == null ? null : v.getDevice().getName(), v.getStatus(), v.getFixedAt())));
            }
            case "incidents" -> {
                sb.append(Csv.row("Fecha", "Severidad", "Título", "Estado", "Dispositivo", "Responsable", "Creado por", "Cerrado"));
                incidents.findByCompanyIdOrderByCreatedAtDesc(cid).forEach(i -> sb.append(Csv.row(i.getCreatedAt(),
                        i.getSeverity(), i.getTitle(), i.getStatus(),
                        i.getDevice() == null ? null : i.getDevice().getName(),
                        i.getAssignee() == null ? null : i.getAssignee().getFullName(),
                        i.getCreatedBy() == null ? null : i.getCreatedBy().getFullName(), i.getClosedAt())));
            }
            default -> { return Optional.empty(); }
        }
        return Optional.of(sb.toString());
    }

    private static <T, E extends Enum<E>> Map<String, Long> count(List<T> items, E[] values, Function<T, E> f) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (E v : values) m.put(v.name(), 0L);
        for (T i : items) m.merge(f.apply(i).name(), 1L, Long::sum);
        return m;
    }
}
