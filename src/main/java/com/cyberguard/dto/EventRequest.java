package com.cyberguard.dto;

import com.cyberguard.entity.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventRequest(
        @NotBlank(message = "El evento necesita un título") @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull(message = "Indique la severidad") AlertSeverity severity) {}
