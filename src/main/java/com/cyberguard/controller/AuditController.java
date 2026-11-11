package com.cyberguard.controller;

import com.cyberguard.dto.AuditDto;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.AuditService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditService service;

    public AuditController(AuditService service) { this.service = service; }

    @GetMapping
    public List<AuditDto> latest(@AuthenticationPrincipal AppUserDetails actor) { return service.latest(actor); }
}
