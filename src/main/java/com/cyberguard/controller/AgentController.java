package com.cyberguard.controller;

import com.cyberguard.dto.EventRequest;
import com.cyberguard.dto.HeartbeatRequest;
import com.cyberguard.security.AgentPrincipal;
import com.cyberguard.service.AgentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService service;

    public AgentController(AgentService service) { this.service = service; }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(@AuthenticationPrincipal AgentPrincipal agent,
                                         @Valid @RequestBody HeartbeatRequest req) {
        service.heartbeat(agent, req);
        return Map.of("ok", true, "serverTime", Instant.now().toString());
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> event(@AuthenticationPrincipal AgentPrincipal agent,
                                     @Valid @RequestBody EventRequest req) {
        service.event(agent, req);
        return Map.of("ok", true);
    }
}
