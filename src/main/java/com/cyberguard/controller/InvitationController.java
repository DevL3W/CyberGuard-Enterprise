package com.cyberguard.controller;

import com.cyberguard.dto.InvitationCreated;
import com.cyberguard.dto.InvitationDto;
import com.cyberguard.dto.InvitationRequest;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.InvitationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Gestión de invitaciones: solo administradores (ver SecurityConfig). */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private final InvitationService service;

    public InvitationController(InvitationService service) { this.service = service; }

    @GetMapping
    public List<InvitationDto> list(@AuthenticationPrincipal AppUserDetails actor) { return service.list(actor); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationCreated create(@AuthenticationPrincipal AppUserDetails actor, @Valid @RequestBody InvitationRequest req) {
        return service.create(actor, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) { service.revoke(actor, id); }
}
