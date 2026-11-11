package com.cyberguard.dto;

import com.cyberguard.entity.DeviceStatus;
import com.cyberguard.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank(message = "Escriba el nombre del dispositivo") @Size(max = 100) String name,
        @Size(max = 150) String hostname,
        @Size(max = 45) @Pattern(regexp = "^$|^[0-9a-fA-F:.]+$", message = "La dirección IP no es válida") String ipAddress,
        @NotNull(message = "Elija el tipo de dispositivo") DeviceType type,
        @Size(max = 100) String operatingSystem,
        @NotNull(message = "Elija el estado del dispositivo") DeviceStatus status) {}
