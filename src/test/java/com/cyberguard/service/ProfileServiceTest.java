package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.PasswordChangeRequest;
import com.cyberguard.dto.ProfileUpdateRequest;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.AuditLogRepository;
import com.cyberguard.repository.IncidentRepository;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class ProfileServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final IncidentRepository incidents = mock(IncidentRepository.class);
    private final AuditLogRepository auditLogs = mock(AuditLogRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService("una-clave-de-prueba-con-mas-de-32-caracteres!!", 5);
    private final ProfileService service =
            new ProfileService(users, incidents, auditLogs, mock(AuditService.class), encoder, jwt);

    private final Company company = new Company("Acme", "K-1");
    private final AppUser user = new AppUser(company, "ana", encoder.encode("clave-actual-123"), "Ana Pérez", Role.ANALISTA);
    private final AppUserDetails actor = new AppUserDetails(1L, 10L, "Acme", "ana", "h", "Ana", Role.ANALISTA, true);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(user, "id", 1L);
        when(users.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(user));
    }

    @Test
    void actualizarPerfilLimpiaEspaciosYVaciosSeGuardanComoNulos() {
        var p = service.update(actor, new ProfileUpdateRequest("  Ana María Pérez ", "  ", "Analista SOC"));
        assertEquals("Ana María Pérez", p.fullName());
        assertNull(p.email());
        assertEquals("Analista SOC", p.jobTitle());
        assertEquals("Acme", p.companyName());
    }

    @Test
    void cambiarContrasenaGuardaHashNuevoFechaYDevuelveToken() {
        var res = service.changePassword(actor, new PasswordChangeRequest("clave-actual-123", "clave-nueva-4567"));
        assertTrue(encoder.matches("clave-nueva-4567", user.getPasswordHash()));
        assertNotNull(user.getPasswordChangedAt());
        assertFalse(res.token().isBlank());
        // El token nuevo es aceptado por la propia cuenta y uno anterior al cambio, no
        var details = AppUserDetails.from(user);
        var issued = jwt.validate(res.token()).orElseThrow().issuedAt();
        assertTrue(details.acceptsTokenIssuedAt(issued));
        assertFalse(details.acceptsTokenIssuedAt(user.getPasswordChangedAt().minusSeconds(60)));
    }

    @Test
    void contrasenaActualIncorrectaSeRechaza() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.changePassword(actor, new PasswordChangeRequest("equivocada-123", "clave-nueva-4567")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertNull(user.getPasswordChangedAt());
    }

    @Test
    void contrasenaNuevaIgualALaActualSeRechaza() {
        assertThrows(ResponseStatusException.class,
                () -> service.changePassword(actor, new PasswordChangeRequest("clave-actual-123", "clave-actual-123")));
    }

    @Test
    void sesionInexistenteDa401() {
        when(users.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class, () -> service.get(actor));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
