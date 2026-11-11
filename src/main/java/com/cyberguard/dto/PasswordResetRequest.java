package com.cyberguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank @Size(min = 10, max = 128, message = "La contraseña debe tener entre 10 y 128 caracteres")
        String newPassword) {}
