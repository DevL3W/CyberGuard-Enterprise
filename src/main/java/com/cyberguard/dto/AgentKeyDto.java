package com.cyberguard.dto;

/** La clave en claro solo se devuelve una vez, al generarla. */
public record AgentKeyDto(Long deviceId, String key) {}
