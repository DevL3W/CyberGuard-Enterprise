package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.AlertRequest;
import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.Role;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AppUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class AlertServiceTest {
    private final AlertRepository alerts = mock(AlertRepository.class);
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final AlertService service = new AlertService(alerts, devices, companies, mock(AuditService.class));
    private final Company company = new Company("Acme", "K-1");
    private final AppUserDetails actor = new AppUserDetails(1L, 10L, "Acme", "ana", "h", "Ana", Role.ANALISTA, true);

    @BeforeEach
    void setUp() {
        when(companies.getReferenceById(10L)).thenReturn(company);
        when(alerts.save(any(SecurityAlert.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void crearConDispositivoDeLaEmpresa() {
        Device d = new Device(company);
        d.setName("Servidor web");
        ReflectionTestUtils.setField(d, "id", 7L);
        when(devices.findByIdAndCompanyId(7L, 10L)).thenReturn(Optional.of(d));
        var dto = service.create(actor, new AlertRequest(" Intento de acceso ", "  ", AlertSeverity.ALTA, 7L));
        assertEquals("Intento de acceso", dto.title());
        assertNull(dto.description());
        assertEquals("Servidor web", dto.deviceName());
        assertEquals(AlertStatus.ABIERTA, dto.status());
    }

    @Test
    void dispositivoDeOtraEmpresaSeRechaza() {
        when(devices.findByIdAndCompanyId(99L, 10L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.create(actor, new AlertRequest("X", null, AlertSeverity.BAJA, 99L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(alerts, never()).save(any());
    }

    @Test
    void resolverRegistraFechaYReabrirLaBorra() {
        SecurityAlert a = new SecurityAlert(company, null, "Alerta", null, AlertSeverity.MEDIA);
        when(alerts.findByIdAndCompanyId(3L, 10L)).thenReturn(Optional.of(a));

        var resuelta = service.changeStatus(actor, 3L, AlertStatus.RESUELTA);
        assertEquals(AlertStatus.RESUELTA, resuelta.status());
        assertNotNull(resuelta.resolvedAt());

        var reabierta = service.changeStatus(actor, 3L, AlertStatus.ABIERTA);
        assertNull(reabierta.resolvedAt());
    }

    @Test
    void alertaDeOtraEmpresaDa404() {
        when(alerts.findByIdAndCompanyId(3L, 10L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.changeStatus(actor, 3L, AlertStatus.RESUELTA));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
