package com.cyberguard.dto;

import com.cyberguard.entity.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlertRequest(
        @NotBlank(message = "Escriba el título de la alerta") @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull(message = "Elija la severidad") AlertSeverity severity,
        Long deviceId) {}
