package com.cyberguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinRequest(
        @NotBlank(message = "Escriba su nombre completo") @Size(max = 150) String fullName,
        @NotBlank @Size(min = 3, max = 50, message = "El nombre de la sesión debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Use solo letras, números, punto, guion y guion bajo")
        String username,
        @NotBlank @Size(min = 10, max = 128, message = "La contraseña debe tener entre 10 y 128 caracteres")
        String password) {}
