package com.cyberguard.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Información básica y permitida que envía el agente: nombre de host, IP y sistema operativo. */
public record HeartbeatRequest(
        @Size(max = 150) String hostname,
        @Size(max = 45) @Pattern(regexp = "^$|^[0-9a-fA-F:.]+$", message = "La dirección IP no es válida") String ipAddress,
        @Size(max = 100) String operatingSystem) {}
