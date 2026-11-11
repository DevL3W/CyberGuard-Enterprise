package com.cyberguard.controller;

import com.cyberguard.dto.AlertDto;
import com.cyberguard.dto.AlertRequest;
import com.cyberguard.dto.AlertStatusRequest;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.AlertService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) { this.service = service; }

    @GetMapping
    public List<AlertDto> list(@AuthenticationPrincipal AppUserDetails actor) { return service.list(actor); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertDto create(@AuthenticationPrincipal AppUserDetails actor, @Valid @RequestBody AlertRequest req) {
        return service.create(actor, req);
    }

    @PatchMapping("/{id}/status")
    public AlertDto changeStatus(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                                 @Valid @RequestBody AlertStatusRequest req) {
        return service.changeStatus(actor, id, req.status());
    }
}
