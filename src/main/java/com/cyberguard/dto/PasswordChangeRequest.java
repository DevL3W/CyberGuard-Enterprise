package com.cyberguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "Escriba su contraseña actual") @Size(max = 128) String currentPassword,
        @NotBlank @Size(min = 10, max = 128, message = "La contraseña nueva debe tener entre 10 y 128 caracteres")
        String newPassword) {}
