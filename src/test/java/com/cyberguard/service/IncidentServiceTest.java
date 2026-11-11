package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.IncidentRequest;
import com.cyberguard.dto.IncidentUpdateRequest;
import com.cyberguard.entity.AlertSeverity;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Incident;
import com.cyberguard.entity.IncidentStatus;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.repository.IncidentRepository;
import com.cyberguard.security.AppUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class IncidentServiceTest {
    private final IncidentRepository incidents = mock(IncidentRepository.class);
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final IncidentService service =
            new IncidentService(incidents, devices, users, companies, mock(AuditService.class));
    private final Company company = new Company("Acme", "K-1");
    private final AppUserDetails actor = new AppUserDetails(1L, 10L, "Acme", "ana", "h", "Ana", Role.ANALISTA, true);
    private final AppUser carlos = new AppUser(company, "carlos", "h", "Carlos", Role.ANALISTA);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(carlos, "id", 7L);
        when(companies.getReferenceById(10L)).thenReturn(company);
        when(incidents.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void crearConResponsableValido() {
        when(users.findByIdAndCompanyId(7L, 10L)).thenReturn(Optional.of(carlos));
        var dto = service.create(actor, new IncidentRequest(" Ransomware en PC ", null, AlertSeverity.CRITICA, null, 7L));
        assertEquals("Ransomware en PC", dto.title());
        assertEquals("Carlos", dto.assigneeName());
        assertEquals(IncidentStatus.ABIERTO, dto.status());
    }

    @Test
    void responsableDeOtraEmpresaSeRechaza() {
        when(users.findByIdAndCompanyId(99L, 10L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.create(actor, new IncidentRequest("X", null, AlertSeverity.BAJA, null, 99L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void responsableDesactivadoSeRechaza() {
        carlos.setEnabled(false);
        when(users.findByIdAndCompanyId(7L, 10L)).thenReturn(Optional.of(carlos));
        assertThrows(ResponseStatusException.class,
                () -> service.create(actor, new IncidentRequest("X", null, AlertSeverity.BAJA, null, 7L)));
    }

    @Test
    void cerrarRegistraFechaYReabrirLaBorra() {
        Incident i = new Incident(company, null, null, null, "X", null, AlertSeverity.MEDIA);
        when(incidents.findByIdAndCompanyId(3L, 10L)).thenReturn(Optional.of(i));
        assertNotNull(service.update(actor, 3L, new IncidentUpdateRequest(IncidentStatus.CERRADO, null)).closedAt());
        assertNull(service.update(actor, 3L, new IncidentUpdateRequest(IncidentStatus.EN_INVESTIGACION, null)).closedAt());
    }

    @Test
    void asignarResponsableSinCambiarEstado() {
        Incident i = new Incident(company, null, null, null, "X", null, AlertSeverity.MEDIA);
        when(incidents.findByIdAndCompanyId(3L, 10L)).thenReturn(Optional.of(i));
        when(users.findByIdAndCompanyId(7L, 10L)).thenReturn(Optional.of(carlos));
        var dto = service.update(actor, 3L, new IncidentUpdateRequest(null, 7L));
        assertEquals("Carlos", dto.assigneeName());
        assertEquals(IncidentStatus.ABIERTO, dto.status());
    }

    @Test
    void incidenteDeOtraEmpresaDa404() {
        when(incidents.findByIdAndCompanyId(3L, 10L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.update(actor, 3L, new IncidentUpdateRequest(IncidentStatus.CERRADO, null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
