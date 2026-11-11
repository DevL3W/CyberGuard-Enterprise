package com.cyberguard.controller;

import com.cyberguard.dto.IncidentDto;
import com.cyberguard.dto.IncidentRequest;
import com.cyberguard.dto.IncidentUpdateRequest;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.IncidentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService service;

    public IncidentController(IncidentService service) { this.service = service; }

    @GetMapping
    public List<IncidentDto> list(@AuthenticationPrincipal AppUserDetails actor) { return service.list(actor); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentDto create(@AuthenticationPrincipal AppUserDetails actor, @Valid @RequestBody IncidentRequest req) {
        return service.create(actor, req);
    }

    @PatchMapping("/{id}")
    public IncidentDto update(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                              @RequestBody IncidentUpdateRequest req) {
        return service.update(actor, id, req);
    }
}
