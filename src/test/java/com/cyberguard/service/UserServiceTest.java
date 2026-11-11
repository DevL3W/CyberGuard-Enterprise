package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.CreateUserRequest;
import com.cyberguard.dto.UpdateUserRequest;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.security.AppUserDetails;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class UserServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final UserService service = new UserService(users, companies, new BCryptPasswordEncoder(4), mock(AuditService.class), "admin");

    private final Company company = new Company("Acme", "K-1");
    private final AppUser admin = new AppUser(company, "admin", "h", "Administrador", Role.ADMIN);
    private final AppUser maria = new AppUser(company, "maria", "h", "María", Role.LECTOR);
    // otro administrador que actúa sobre las demás sesiones
    private final AppUserDetails actor = new AppUserDetails(2L, 10L, "Acme", "carlos", "h", "Carlos", Role.ADMIN, true);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(maria, "id", 3L);
        when(users.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(admin));
        when(users.findByIdAndCompanyId(3L, 10L)).thenReturn(Optional.of(maria));
        when(companies.getReferenceById(10L)).thenReturn(company);
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void sesionAdminNoSePuedeDesactivar() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.update(actor, 1L, new UpdateUserRequest(null, false)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(admin.isEnabled());
    }

    @Test
    void sesionAdminNoPuedeCambiarDeRol() {
        assertThrows(ResponseStatusException.class,
                () -> service.update(actor, 1L, new UpdateUserRequest(Role.LECTOR, null)));
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    void otraSesionSiSePuedeDesactivar() {
        var dto = service.update(actor, 3L, new UpdateUserRequest(null, false));
        assertFalse(dto.enabled());
        assertFalse(dto.protectedAccount());
    }

    @Test
    void sesionPrincipalSeMarcaComoProtegida() {
        assertTrue(service.update(actor, 1L, new UpdateUserRequest(null, null)).protectedAccount());
    }

    @Test
    void crearSesionGuardaHashYNormalizaNombre() {
        var dto = service.create(actor, new CreateUserRequest("Pedro.Ruiz", "contraseña-larga-1", "Pedro Ruiz", Role.ANALISTA));
        assertEquals("pedro.ruiz", dto.username());
        verify(users).save(argThat(u -> !u.getPasswordHash().equals("contraseña-larga-1")
                && u.getPasswordHash().startsWith("$2")));
    }

    @Test
    void nombreDuplicadoDaConflicto() {
        when(users.existsByUsername("maria")).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.create(actor, new CreateUserRequest("maria", "contraseña-larga-1", "Otra", Role.LECTOR)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void restablecerClaveDeLaSesionPrincipalPorOtroAdminSeRechaza() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(actor, 1L, "nueva-clave-segura-1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertNull(admin.getPasswordChangedAt());
    }

    @Test
    void restablecerClaveActualizaHashYFecha() {
        service.resetPassword(actor, 3L, "nueva-clave-segura-1");
        assertTrue(new BCryptPasswordEncoder(4).matches("nueva-clave-segura-1", maria.getPasswordHash()));
        assertNotNull(maria.getPasswordChangedAt());
    }

    @Test
    void nadiePuedeRestablecerSuPropiaClavePorAqui() {
        when(users.findByIdAndCompanyId(2L, 10L)).thenReturn(Optional.of(maria));
        ReflectionTestUtils.setField(maria, "id", 2L);
        assertThrows(ResponseStatusException.class, () -> service.resetPassword(actor, 2L, "nueva-clave-segura-1"));
    }
}
