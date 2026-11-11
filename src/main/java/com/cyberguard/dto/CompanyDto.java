package com.cyberguard.dto;

import java.time.Instant;

public record CompanyDto(String name, String licenseKey, boolean active, Instant createdAt,
                         long users, long devices) {}
