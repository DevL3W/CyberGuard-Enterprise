package com.cyberguard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Escriba su nombre completo") @Size(max = 150) String fullName,
        @Email(message = "El correo electrónico no es válido") @Size(max = 150) String email,
        @Size(max = 100) String jobTitle) {}
