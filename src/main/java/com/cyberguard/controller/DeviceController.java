package com.cyberguard.controller;

import com.cyberguard.dto.AgentKeyDto;
import com.cyberguard.dto.DeviceDto;
import com.cyberguard.dto.DeviceRequest;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.DeviceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService service;

    public DeviceController(DeviceService service) { this.service = service; }

    @GetMapping
    public List<DeviceDto> list(@AuthenticationPrincipal AppUserDetails actor) { return service.list(actor); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceDto create(@AuthenticationPrincipal AppUserDetails actor, @Valid @RequestBody DeviceRequest req) {
        return service.create(actor, req);
    }

    @PutMapping("/{id}")
    public DeviceDto update(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id,
                            @Valid @RequestBody DeviceRequest req) {
        return service.update(actor, id, req);
    }

    /** Genera una clave nueva para el agente del dispositivo; se muestra una sola vez. */
    @PostMapping("/{id}/agent-key")
    public AgentKeyDto agentKey(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) {
        return service.generateAgentKey(actor, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails actor, @PathVariable Long id) {
        service.delete(actor, id);
    }
}
