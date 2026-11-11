package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cyberguard.dto.InvitationRequest;
import com.cyberguard.dto.JoinRequest;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Company;
import com.cyberguard.entity.Invitation;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.repository.CompanyRepository;
import com.cyberguard.repository.InvitationRepository;
import com.cyberguard.security.AgentKeys;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.security.JoinRateLimiter;
import com.cyberguard.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class InvitationServiceTest {
    private final InvitationRepository invitations = mock(InvitationRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final JwtService jwt = new JwtService("una-clave-de-prueba-con-mas-de-32-caracteres!!", 5);
    private final Company company = new Company("Acme", "K-1");
    private final AppUserDetails admin = new AppUserDetails(1L, 10L, "Acme", "admin", "h", "Administrador", Role.ADMIN, true);
    private final JoinRequest join = new JoinRequest("Luisa Gómez", "Luisa.Gomez", "contraseña-larga-1");
    private final String token = AgentKeys.generate();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(company, "id", 10L);
        when(companies.findById(10L)).thenReturn(Optional.of(company));
        when(invitations.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));
        when(users.save(any(AppUser.class))).thenAnswer(i -> {
            AppUser u = i.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 99L);
            return u;
        });
    }

    private InvitationService service(int maxJoinsPerHour) {
        return new InvitationService(invitations, companies, users, new BCryptPasswordEncoder(4), jwt,
                new JoinRateLimiter(maxJoinsPerHour), mock(AuditService.class));
    }

    private Invitation stored(Role role, int maxUses, Instant expiresAt) {
        Invitation inv = new Invitation(10L, AgentKeys.hash(token), role, maxUses, expiresAt, 1L, "admin");
        ReflectionTestUtils.setField(inv, "id", 5L);
        when(invitations.findByTokenHash(AgentKeys.hash(token))).thenReturn(Optional.of(inv));
        return inv;
    }

    @Test
    void crearGuardaSoloLaHuellaYNoElToken() {
        var created = service(20).create(admin, new InvitationRequest(Role.LECTOR, 7, 5));
        assertEquals(43, created.token().length());
        verify(invitations).save(argThat(i -> i.getTokenHash().equals(AgentKeys.hash(created.token()))
                && !i.getTokenHash().equals(created.token())
                && i.getCompanyId().equals(10L) && i.getMaxUses() == 5));
        assertEquals("ACTIVA", created.invitation().status());
    }

    @Test
    void unaInvitacionNuncaPuedeDarRolDeAdministrador() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service(20).create(admin, new InvitationRequest(Role.ADMIN, 7, 5)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(invitations, never()).save(any());
    }

    @Test
    void unirseCreaLaSesionConElRolDeLaInvitacion() {
        stored(Role.ANALISTA, 5, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitations.consume(eq(5L), any())).thenReturn(1);
        var res = service(20).accept("1.2.3.4", token, join);
        assertEquals("luisa.gomez", res.user().username());
        assertEquals(Role.ANALISTA, res.user().role());
        assertEquals("Acme", res.user().companyName());
        assertFalse(res.token().isBlank());
        verify(users).save(argThat(u -> u.getPasswordHash().startsWith("$2")
                && !u.getPasswordHash().contains("contraseña-larga-1")));
    }

    @Test
    void sinUsosDisponiblesNoSeCreaLaSesion() {
        stored(Role.LECTOR, 1, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitations.consume(anyLong(), any())).thenReturn(0); // otra persona gastó el último uso
        var ex = assertThrows(ResponseStatusException.class, () -> service(20).accept("1.2.3.4", token, join));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(users, never()).save(any());
    }

    @Test
    void invitacionVencidaRevocadaOAgotadaSeRechazan() {
        stored(Role.LECTOR, 5, Instant.now().minus(1, ChronoUnit.HOURS));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service(20).preview(token)).getStatusCode());

        Invitation revocada = stored(Role.LECTOR, 5, Instant.now().plus(1, ChronoUnit.DAYS));
        revocada.setRevoked(true);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service(20).preview(token)).getStatusCode());

        Invitation agotada = stored(Role.LECTOR, 1, Instant.now().plus(1, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(agotada, "uses", 1);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service(20).preview(token)).getStatusCode());
    }

    @Test
    void enlaceDesconocidoOMalFormadoDaElMismoError() {
        when(invitations.findByTokenHash(any())).thenReturn(Optional.empty());
        var a = assertThrows(ResponseStatusException.class, () -> service(20).preview(AgentKeys.generate()));
        var b = assertThrows(ResponseStatusException.class, () -> service(20).preview("corto"));
        assertEquals(a.getReason(), b.getReason());
        assertEquals(HttpStatus.NOT_FOUND, a.getStatusCode());
    }

    @Test
    void nombreDeSesionRepetidoDaConflictoSinGastarElUso() {
        stored(Role.LECTOR, 5, Instant.now().plus(1, ChronoUnit.DAYS));
        when(users.existsByUsername("luisa.gomez")).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> service(20).accept("1.2.3.4", token, join));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(invitations, never()).consume(anyLong(), any());
    }

    @Test
    void demasiadosIntentosDesdeUnaConexionSeLimitan() {
        stored(Role.LECTOR, 5, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitations.consume(anyLong(), any())).thenReturn(1);
        var service = service(1);
        service.accept("1.2.3.4", token, join);
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.accept("1.2.3.4", token, new JoinRequest("Otra", "otra.sesion", "contraseña-larga-1")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void revocarMarcaLaInvitacionDeLaPropiaEmpresa() {
        Invitation inv = stored(Role.LECTOR, 5, Instant.now().plus(1, ChronoUnit.DAYS));
        when(invitations.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of(inv));
        service(20).revoke(admin, 5L);
        assertTrue(inv.isRevoked());
    }

    @Test
    void noSePuedeRevocarLaInvitacionDeOtraEmpresa() {
        when(invitations.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.empty());
        var ex = assertThrows(ResponseStatusException.class, () -> service(20).revoke(admin, 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
