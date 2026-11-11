package com.cyberguard.controller;

import com.cyberguard.dto.DirectoryEntry;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.UserService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista corta de sesiones activas de la empresa, para elegir responsables. Disponible para todos los roles. */
@RestController
@RequestMapping("/api/directory")
public class DirectoryController {
    private final UserService service;

    public DirectoryController(UserService service) { this.service = service; }

    @GetMapping
    public List<DirectoryEntry> list(@AuthenticationPrincipal AppUserDetails actor) { return service.directory(actor); }
}
