package com.cyberguard.controller;

import com.cyberguard.dto.LoginRequest;
import com.cyberguard.dto.LoginResponse;
import com.cyberguard.dto.UserDto;
import com.cyberguard.entity.AppUser;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.security.JwtService;
import com.cyberguard.security.LoginAttemptLimiter;
import com.cyberguard.service.AuditService;
import com.cyberguard.service.GuestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final AppUserRepository users;
    private final AuditService audit;
    private final LoginAttemptLimiter limiter;
    private final GuestService guest;

    public AuthController(AuthenticationManager authManager, JwtService jwt, AppUserRepository users,
                          AuditService audit, LoginAttemptLimiter limiter, GuestService guest) {
        this.authManager = authManager; this.jwt = jwt; this.users = users;
        this.audit = audit; this.limiter = limiter; this.guest = guest;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String username = req.username().trim().toLowerCase();
        String key = http.getRemoteAddr() + "|" + username;
        if (limiter.isBlocked(key)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espere unos 15 minutos e inténtelo de nuevo");
        }
        Authentication auth;
        try {
            auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(username, req.password()));
        } catch (AuthenticationException e) {
            limiter.failure(key);
            throw e;
        }
        limiter.success(key);
        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        AppUser user = users.findById(principal.getId()).orElseThrow();
        audit.record(principal, "INICIO_SESION", "Sesión", principal.getId(), null);
        return new LoginResponse(jwt.generate(principal), jwt.getExpirationMinutes(), UserDto.from(user));
    }

    /** Modo invitado: sesión de solo consulta, sin escribir usuario ni contraseña. */
    @PostMapping("/guest")
    public LoginResponse guest() {
        return guest.login();
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AppUserDetails principal) {
        return users.findById(principal.getId()).map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
