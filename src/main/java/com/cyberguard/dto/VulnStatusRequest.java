package com.cyberguard.dto;

import com.cyberguard.entity.VulnStatus;
import jakarta.validation.constraints.NotNull;

public record VulnStatusRequest(@NotNull(message = "Indique el nuevo estado") VulnStatus status) {}
