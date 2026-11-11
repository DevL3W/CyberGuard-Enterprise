package com.cyberguard.controller;

import com.cyberguard.dto.LoginResponse;
import com.cyberguard.dto.RegisterRequest;
import com.cyberguard.service.GuestService;
import com.cyberguard.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private final RegistrationService service;
    private final GuestService guest;

    public RegistrationController(RegistrationService service, GuestService guest) {
        this.service = service; this.guest = guest;
    }

    /** Permite a la pantalla de inicio saber si debe mostrar el enlace "Crear cuenta". */
    @GetMapping("/config")
    public Map<String, Boolean> config() {
        return Map.of("registrationEnabled", service.isEnabled(), "guestEnabled", guest.isEnabled());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        // Se usa la dirección de la conexión (no cabeceras X-Forwarded-For, que el cliente puede falsear)
        return service.register(http.getRemoteAddr(), req);
    }
}
