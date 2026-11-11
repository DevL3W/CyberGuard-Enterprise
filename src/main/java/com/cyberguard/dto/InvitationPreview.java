package com.cyberguard.dto;

import com.cyberguard.entity.Role;
import java.time.Instant;

public record InvitationPreview(String companyName, Role role, Instant expiresAt) {}
