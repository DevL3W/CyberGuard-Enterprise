package com.cyberguard.controller;

import com.cyberguard.dto.ReportOverview;
import com.cyberguard.security.AppUserDetails;
import com.cyberguard.service.ReportService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/overview")
    public ReportOverview overview(@AuthenticationPrincipal AppUserDetails actor) { return service.overview(actor); }

    @GetMapping("/export/{name}")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AppUserDetails actor, @PathVariable String name) {
        String csv = service.csv(name, actor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Informe no encontrado"));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cyberguard-" + name + ".csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
