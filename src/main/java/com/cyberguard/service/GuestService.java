package com.cyberguard.service;

import com.cyberguard.dto.LoginResponse;
import com.cyberguard.dto.UserDto;
import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Role;
import com.cyberguard.repository.AppUserRepository;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Modo invitado: entrar solo para ver. Emite una sesión de la cuenta de invitado, que siempre tiene
 * el rol LECTOR; el servidor rechaza cualquier escritura de ese rol (ver SecurityConfig).
 */
@Service
public class GuestService {
    private final AppUserRepository users;
    private final JwtService jwt;
    private final AuditService audit;
    private final boolean enabled;
    private final String username;

    public GuestService(AppUserRepository users, JwtService jwt, AuditService audit,
                        @Value("${cyberguard.guest.enabled:true}") boolean enabled,
                        @Value("${cyberguard.guest.username:invitado}") String username) {
        this.users = users; this.jwt = jwt; this.audit = audit;
        this.enabled = enabled; this.username = username.trim().toLowerCase();
    }

    public boolean isEnabled() { return enabled; }
    public String getUsername() { return username; }

    /** La cuenta de invitado es compartida: nadie puede cambiar su perfil ni su contraseña. */
    public void denyIfGuest(AppUserDetails actor) {
        if (enabled && username.equals(actor.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta de invitado es de solo lectura");
        }
    }

    @Transactional
    public LoginResponse login() {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El modo invitado está desactivado");
        }
        AppUser guest = users.findByUsername(username)
                .filter(u -> u.getRole() == Role.LECTOR && u.isEnabled())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "El modo invitado no está disponible en este momento"));
        AppUserDetails principal = AppUserDetails.from(guest);
        if (!principal.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El modo invitado no está disponible en este momento");
        }
        audit.record(principal, "INICIO_SESION_INVITADO", "Sesión", principal.getId(), null);
        return new LoginResponse(jwt.generate(principal), jwt.getExpirationMinutes(), UserDto.from(guest));
    }
}
