package com.cyberguard.dto;

import com.cyberguard.entity.Invitation;
import com.cyberguard.entity.Role;
import java.time.Instant;

/** status: ACTIVA, REVOCADA, VENCIDA o AGOTADA. */
public record InvitationDto(Long id, Role role, int maxUses, int uses, Instant expiresAt,
                            String createdBy, Instant createdAt, String status) {
    public static InvitationDto from(Invitation i, Instant now) {
        String status = i.isRevoked() ? "REVOCADA"
                : !i.getExpiresAt().isAfter(now) ? "VENCIDA"
                : i.getUses() >= i.getMaxUses() ? "AGOTADA" : "ACTIVA";
        return new InvitationDto(i.getId(), i.getRole(), i.getMaxUses(), i.getUses(), i.getExpiresAt(),
                i.getCreatedByName(), i.getCreatedAt(), status);
    }
}
