package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.DeviceRequest;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Device;
import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.DeviceType;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AppUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DeviceServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final DeviceService service = new DeviceService(devices, companies, mock(AuditService.class));
    private final Company company = new Company("Acme", "K-1");
    private final AppUserDetails actor = new AppUserDetails(1L, 10L, "Acme", "ana", "h", "Ana", Role.ADMIN, true);

    @BeforeEach
    void setUp() {
        when(companies.getReferenceById(10L)).thenReturn(company);
        when(devices.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void crearLimpiaEspaciosYGuardaDatos() {
        var dto = service.create(actor, new DeviceRequest(" Servidor web ", "web01", "10.0.0.5",
                DeviceType.SERVIDOR, "Ubuntu 24.04", DeviceStatus.ONLINE));
        assertEquals("Servidor web", dto.name());
        assertEquals(DeviceStatus.ONLINE, dto.status());
    }

    @Test
    void camposVaciosSeGuardanComoNulos() {
        var dto = service.create(actor, new DeviceRequest("PC", "  ", "", DeviceType.COMPUTADOR, null, DeviceStatus.OFFLINE));
        assertNull(dto.hostname());
        assertNull(dto.ipAddress());
        assertNull(dto.operatingSystem());
    }

    @Test
    void dispositivoDeOtraEmpresaDa404() {
        when(devices.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.empty());
        var req = new DeviceRequest("PC", null, null, DeviceType.COMPUTADOR, null, DeviceStatus.OFFLINE);
        var ex = assertThrows(ResponseStatusException.class, () -> service.update(actor, 5L, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertThrows(ResponseStatusException.class, () -> service.delete(actor, 5L));
    }

    @Test
    void eliminarBorraElDispositivo() {
        Device d = new Device(company);
        when(devices.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of(d));
        service.delete(actor, 5L);
        verify(devices).delete(d);
    }
}
