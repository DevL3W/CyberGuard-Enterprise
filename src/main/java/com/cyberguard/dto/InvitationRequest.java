package com.cyberguard.dto;

import com.cyberguard.entity.Role;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InvitationRequest(
        @NotNull(message = "Elija el rol de la invitación") Role role,
        @Min(value = 1, message = "La validez debe ser de 1 a 30 días")
        @Max(value = 30, message = "La validez debe ser de 1 a 30 días") int days,
        @Min(value = 1, message = "Los usos deben estar entre 1 y 100")
        @Max(value = 100, message = "Los usos deben estar entre 1 y 100") int maxUses) {}
