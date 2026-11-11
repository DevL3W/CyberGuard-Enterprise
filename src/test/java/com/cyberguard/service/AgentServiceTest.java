package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.EventRequest;
import com.cyberguard.dto.HeartbeatRequest;
import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AlertStatus;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.SecurityAlert;
import com.cyberguard.repository.AlertRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AgentKeys;
import com.cyberguard.security.AgentPrincipal;
import com.cyberguard.security.AgentRateLimiter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class AgentServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final AlertRepository alerts = mock(AlertRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final Company company = new Company("Acme", "K-1");
    private final Device device = new Device(company);
    private final AgentPrincipal principal = new AgentPrincipal(5L, 10L, "Servidor");

    private AgentService service(int maxEventsPerMinute) {
        return new AgentService(devices, alerts, audit, new AgentRateLimiter(maxEventsPerMinute));
    }

    @BeforeEach
    void setUp() {
        device.setName("Servidor");
        device.setStatus(DeviceStatus.OFFLINE);
        device.setAgentKeyHash(AgentKeys.hash("clave-correcta"));
        ReflectionTestUtils.setField(device, "id", 5L);
        when(devices.findById(5L)).thenReturn(Optional.of(device));
        when(alerts.save(any(SecurityAlert.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void claveCorrectaAutentica() {
        var p = service(5).authenticate(5L, "clave-correcta");
        assertTrue(p.isPresent());
        assertEquals("Servidor", p.get().deviceName());
    }

    @Test
    void claveIncorrectaNoAutentica() {
        assertTrue(service(5).authenticate(5L, "otra-clave").isEmpty());
    }

    @Test
    void dispositivoSinClaveNoAutentica() {
        device.setAgentKeyHash(null);
        assertTrue(service(5).authenticate(5L, "clave-correcta").isEmpty());
    }

    @Test
    void dispositivoInexistenteNoAutentica() {
        when(devices.findById(99L)).thenReturn(Optional.empty());
        assertTrue(service(5).authenticate(99L, "clave-correcta").isEmpty());
    }

    @Test
    void empresaConLicenciaSuspendidaNoAutentica() {
        company.setActive(false);
        assertTrue(service(5).authenticate(5L, "clave-correcta").isEmpty());
    }

    @Test
    void heartbeatMarcaEnLineaYActualizaDatos() {
        service(5).heartbeat(principal, new HeartbeatRequest("srv01", "10.0.0.9", "Ubuntu 24.04"));
        assertEquals(DeviceStatus.ONLINE, device.getStatus());
        assertNotNull(device.getLastSeen());
        assertEquals("srv01", device.getHostname());
        assertEquals("10.0.0.9", device.getIpAddress());
        assertEquals("Ubuntu 24.04", device.getOperatingSystem());
    }

    @Test
    void heartbeatCierraLasAlertasDeDesconexion() {
        SecurityAlert abierta = new SecurityAlert(company, device, AgentService.DISCONNECT_TITLE, null, AlertSeverity.ALTA);
        when(alerts.findByDeviceIdAndTitleAndStatusNot(5L, AgentService.DISCONNECT_TITLE, AlertStatus.RESUELTA))
                .thenReturn(List.of(abierta));
        service(5).heartbeat(principal, new HeartbeatRequest(null, null, null));
        assertEquals(AlertStatus.RESUELTA, abierta.getStatus());
        assertNotNull(abierta.getResolvedAt());
    }

    @Test
    void dispositivoEnMantenimientoNoCambiaDeEstado() {
        device.setStatus(DeviceStatus.MANTENIMIENTO);
        service(5).heartbeat(principal, new HeartbeatRequest(null, null, null));
        assertEquals(DeviceStatus.MANTENIMIENTO, device.getStatus());
        assertNotNull(device.getLastSeen());
    }

    @Test
    void eventoCreaUnaAlertaDelDispositivo() {
        service(5).event(principal, new EventRequest(" Poco espacio en disco ", "  ", AlertSeverity.ALTA));
        verify(alerts).save(argThat(a -> a.getTitle().equals("Poco espacio en disco")
                && a.getDescription() == null && a.getDevice() == device));
    }

    @Test
    void eventosExcesivosSeLimitan() {
        var svc = service(1);
        svc.event(principal, new EventRequest("Uno", null, AlertSeverity.BAJA));
        var ex = assertThrows(ResponseStatusException.class,
                () -> svc.event(principal, new EventRequest("Dos", null, AlertSeverity.BAJA)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void clavesGeneradasSonUnicasYSeVerificanPorHuella() {
        String a = AgentKeys.generate();
        String b = AgentKeys.generate();
        assertNotEquals(a, b);
        assertTrue(a.length() >= 40);
        assertTrue(AgentKeys.matches(a, AgentKeys.hash(a)));
        assertFalse(AgentKeys.matches(b, AgentKeys.hash(a)));
        assertFalse(AgentKeys.matches(null, AgentKeys.hash(a)));
    }
}
