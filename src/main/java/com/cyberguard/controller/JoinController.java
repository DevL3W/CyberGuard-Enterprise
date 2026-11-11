package com.cyberguard.controller;

import com.cyberguard.dto.InvitationPreview;
import com.cyberguard.dto.JoinRequest;
import com.cyberguard.dto.LoginResponse;
import com.cyberguard.service.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Rutas públicas: quien recibe el enlace o el QR aún no tiene sesión. */
@RestController
@RequestMapping("/api/join")
public class JoinController {
    private final InvitationService service;

    public JoinController(InvitationService service) { this.service = service; }

    @GetMapping("/{token}")
    public InvitationPreview preview(@PathVariable String token) { return service.preview(token); }

    @PostMapping("/{token}")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse join(@PathVariable String token, @Valid @RequestBody JoinRequest req, HttpServletRequest http) {
        // Dirección de la conexión (no X-Forwarded-For, que el cliente puede falsear), como en el registro
        return service.accept(http.getRemoteAddr(), token, req);
    }
}
