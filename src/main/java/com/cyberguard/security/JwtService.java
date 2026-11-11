package com.cyberguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    /** Datos de un token válido: la sesión y cuándo se emitió. */
    public record ValidToken(String username, Instant issuedAt) {}

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${cyberguard.jwt.secret}") String secret,
                      @Value("${cyberguard.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("cyberguard.jwt.secret debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMinutes = expirationMinutes;
    }

    public long getExpirationMinutes() { return expirationMinutes; }

    public String generate(AppUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("cid", user.getCompanyId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expirationMinutes))))
                .signWith(key)
                .compact();
    }

    /** Devuelve los datos del token si la firma es correcta y no ha expirado. */
    public Optional<ValidToken> validate(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (c.getSubject() == null || c.getIssuedAt() == null) return Optional.empty();
            return Optional.of(new ValidToken(c.getSubject(), c.getIssuedAt().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<String> validateAndGetUsername(String token) {
        return validate(token).map(ValidToken::username);
    }
}
