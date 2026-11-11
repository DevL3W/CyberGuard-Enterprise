package com.cyberguard.dto;

import com.cyberguard.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Use solo letras, números, punto, guion y guion bajo")
        String username,
        @NotBlank @Size(min = 10, max = 128, message = "La contraseña debe tener entre 10 y 128 caracteres")
        String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role) {}
