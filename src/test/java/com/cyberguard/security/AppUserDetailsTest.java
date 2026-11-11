package com.cyberguard.security;

import static org.junit.jupiter.api.Assertions.*;

import com.cyberguard.entity.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class AppUserDetailsTest {
    private AppUserDetails user(Instant passwordChangedAt) {
        return new AppUserDetails(1L, 10L, "Acme", "ana", "h", "Ana", Role.ADMIN, true, passwordChangedAt);
    }

    @Test
    void sinCambioDeContrasenaAceptaCualquierToken() {
        assertTrue(user(null).acceptsTokenIssuedAt(Instant.now().minusSeconds(86_400)));
    }

    @Test
    void tokenAnteriorAlCambioSeRechaza() {
        Instant changed = Instant.now();
        assertFalse(user(changed).acceptsTokenIssuedAt(changed.minusSeconds(60)));
    }

    @Test
    void tokenEmitidoEnElMismoSegundoDelCambioSeAcepta() {
        Instant changed = Instant.now();
        assertTrue(user(changed).acceptsTokenIssuedAt(changed.truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void tokenPosteriorAlCambioSeAcepta() {
        Instant changed = Instant.now();
        assertTrue(user(changed).acceptsTokenIssuedAt(changed.plusSeconds(5)));
    }
}
