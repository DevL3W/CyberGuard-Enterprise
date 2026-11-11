package com.cyberguard.dto;

import com.cyberguard.entity.IncidentStatus;

/** Ambos campos son opcionales: solo se cambia lo que venga informado. */
public record IncidentUpdateRequest(IncidentStatus status, Long assigneeId) {}
