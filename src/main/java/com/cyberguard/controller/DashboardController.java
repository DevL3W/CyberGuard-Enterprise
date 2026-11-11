package com.cyberguard.controller;

import com.cyberguard.dto.DashboardSummary;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public DashboardSummary summary(@AuthenticationPrincipal AppUserDetails actor) { return service.summary(actor); }
}
