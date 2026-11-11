package com.cyberguard.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
    @Test
    void bloqueaTrasLosIntentosPermitidos() {
        var limiter = new LoginAttemptLimiter(3);
        for (int i = 0; i < 2; i++) limiter.failure("1.1.1.1|ana");
        assertFalse(limiter.isBlocked("1.1.1.1|ana"));
        limiter.failure("1.1.1.1|ana");
        assertTrue(limiter.isBlocked("1.1.1.1|ana"));
    }

    @Test
    void otraConexionOOtraSesionNoSeVenAfectadas() {
        var limiter = new LoginAttemptLimiter(1);
        limiter.failure("1.1.1.1|ana");
        assertTrue(limiter.isBlocked("1.1.1.1|ana"));
        assertFalse(limiter.isBlocked("2.2.2.2|ana"));
        assertFalse(limiter.isBlocked("1.1.1.1|luis"));
    }

    @Test
    void unInicioCorrectoLimpiaLosIntentos() {
        var limiter = new LoginAttemptLimiter(2);
        limiter.failure("k");
        limiter.success("k");
        limiter.failure("k");
        assertFalse(limiter.isBlocked("k"));
    }
}
