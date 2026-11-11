package com.cyberguard.controller;

import com.cyberguard.dto.CompanyDto;
import com.cyberguard.dto.CompanyUpdateRequest;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) { this.service = service; }

    @GetMapping
    public CompanyDto get(@AuthenticationPrincipal AppUserDetails actor) { return service.get(actor); }

    @PutMapping
    public CompanyDto rename(@AuthenticationPrincipal AppUserDetails actor, @Valid @RequestBody CompanyUpdateRequest req) {
        return service.rename(actor, req.name());
    }
}
