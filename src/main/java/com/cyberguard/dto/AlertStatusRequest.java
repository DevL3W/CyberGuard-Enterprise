package com.cyberguard.dto;

import com.cyberguard.entity.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record AlertStatusRequest(@NotNull(message = "Indique el nuevo estado") AlertStatus status) {}
