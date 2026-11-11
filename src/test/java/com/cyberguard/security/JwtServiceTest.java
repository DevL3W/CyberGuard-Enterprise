package com.cyberguard.security;

import static org.junit.jupiter.api.Assertions.*;

import com.cyberguard.entity.Role;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "una-clave-de-prueba-con-mas-de-32-caracteres!!";

    private AppUserDetails user() {
        return new AppUserDetails(1L, 10L, "Acme", "maria", "hash", "María", Role.ANALISTA, true);
    }

    @Test
    void tokenValidoDevuelveUsuario() {
        JwtService jwt = new JwtService(SECRET, 5);
        assertEquals("maria", jwt.validateAndGetUsername(jwt.generate(user())).orElseThrow());
    }

    @Test
    void tokenManipuladoSeRechaza() {
        JwtService jwt = new JwtService(SECRET, 5);
        assertTrue(jwt.validateAndGetUsername(jwt.generate(user()) + "x").isEmpty());
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() {
        String token = new JwtService("otra-clave-distinta-con-mas-de-32-caracteres!", 5).generate(user());
        assertTrue(new JwtService(SECRET, 5).validateAndGetUsername(token).isEmpty());
    }

    @Test
    void tokenExpiradoSeRechaza() {
        JwtService jwt = new JwtService(SECRET, -1);
        assertTrue(jwt.validateAndGetUsername(jwt.generate(user())).isEmpty());
    }

    @Test
    void claveCortaFalla() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corta", 5));
    }

    @Test
    void validateDevuelveSesionYFechaDeEmision() {
        JwtService jwt = new JwtService(SECRET, 5);
        var token = jwt.validate(jwt.generate(user())).orElseThrow();
        assertEquals("maria", token.username());
        assertFalse(token.issuedAt().isAfter(java.time.Instant.now()));
        assertTrue(token.issuedAt().isAfter(java.time.Instant.now().minusSeconds(10)));
    }
}
