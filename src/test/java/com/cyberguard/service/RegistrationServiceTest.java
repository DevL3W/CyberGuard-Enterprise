package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.RegisterRequest;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.security.JwtService;
import com.cyberguard.security.RegistrationRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class RegistrationServiceTest {
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final JwtService jwt = new JwtService("una-clave-de-prueba-con-mas-de-32-caracteres!!", 5);
    private final RegisterRequest req = new RegisterRequest("Acme SA", "Ana Pérez", "Ana.Perez", "contraseña-larga-1");

    @BeforeEach
    void setUp() {
        when(companies.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
    }

    private RegistrationService service(boolean enabled, int maxPerHour) {
        return new RegistrationService(companies, users, new BCryptPasswordEncoder(4), jwt,
                new RegistrationRateLimiter(maxPerHour), mock(AuditService.class), enabled);
    }

    @Test
    void registroCreaEmpresaYAdministrador() {
        var res = service(true, 5).register("1.2.3.4", req);
        assertEquals("ana.perez", res.user().username());
        assertEquals(Role.ADMIN, res.user().role());
        assertEquals("Acme SA", res.user().companyName());
        assertFalse(res.token().isBlank());
        verify(users).save(argThat(u -> u.getPasswordHash().startsWith("$2")
                && !u.getPasswordHash().contains("contraseña-larga-1")));
    }

    @Test
    void nombreDeSesionExistenteDaConflicto() {
        when(users.existsByUsername("ana.perez")).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service(true, 5).register("1.2.3.4", req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(companies, never()).save(any());
    }

    @Test
    void registroDesactivadoDaProhibido() {
        var ex = assertThrows(ResponseStatusException.class, () -> service(false, 5).register("1.2.3.4", req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void demasiadosIntentosDesdeLaMismaConexionSeBloquean() {
        var svc = service(true, 1);
        svc.register("9.9.9.9", req);
        var ex = assertThrows(ResponseStatusException.class,
                () -> svc.register("9.9.9.9", new RegisterRequest("Otra SA", "Luis", "luis", "contraseña-larga-2")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void otraConexionNoSeVeAfectada() {
        var svc = service(true, 1);
        svc.register("9.9.9.9", req);
        assertDoesNotThrow(() -> svc.register("8.8.8.8",
                new RegisterRequest("Otra SA", "Luis", "luis", "contraseña-larga-2")));
    }
}
