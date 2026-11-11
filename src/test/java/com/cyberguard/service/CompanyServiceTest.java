package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cyberguard.entity.Company;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.DeviceRepository;
import com.cyberguard.security.AppUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CompanyServiceTest {
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final CompanyService service = new CompanyService(companies, users, devices, mock(AuditService.class));
    private final AppUserDetails actor = new AppUserDetails(1L, 10L, "Acme", "ana", "h", "Ana", Role.ADMIN, true);

    @Test
    void renombrarLimpiaEspaciosYDevuelveTotales() {
        Company c = new Company("Acme", "CG-1");
        when(companies.findById(10L)).thenReturn(Optional.of(c));
        when(users.countByCompanyId(10L)).thenReturn(3L);
        when(devices.countByCompanyId(10L)).thenReturn(7L);

        var dto = service.rename(actor, "  Acme Global SA ");

        assertEquals("Acme Global SA", dto.name());
        assertEquals("CG-1", dto.licenseKey());
        assertEquals(3, dto.users());
        assertEquals(7, dto.devices());
    }

    @Test
    void empresaInexistenteDa404() {
        when(companies.findById(10L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class, () -> service.get(actor));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
