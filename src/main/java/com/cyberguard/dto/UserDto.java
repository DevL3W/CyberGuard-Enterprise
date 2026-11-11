package com.cyberguard.dto;

import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Role;
import java.time.Instant;

public record UserDto(Long id, String username, String fullName, Role role, boolean enabled,
                      String companyName, Instant createdAt, boolean protectedAccount) {
    public static UserDto from(AppUser u) { return from(u, false); }

    public static UserDto from(AppUser u, boolean protectedAccount) {
        return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isEnabled(),
                u.getCompany().getName(), u.getCreatedAt(), protectedAccount);
    }
}
