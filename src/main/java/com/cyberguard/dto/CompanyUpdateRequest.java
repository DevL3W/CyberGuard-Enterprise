package com.cyberguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
        @NotBlank(message = "Escriba el nombre de la empresa") @Size(max = 150) String name) {}
