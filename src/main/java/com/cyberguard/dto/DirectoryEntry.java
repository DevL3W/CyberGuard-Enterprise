package com.cyberguard.dto;

import com.cyberguard.entity.AppUser;
import com.cyberguard.entity.Role;

public record DirectoryEntry(Long id, String fullName, Role role) {
    public static DirectoryEntry from(AppUser u) { return new DirectoryEntry(u.getId(), u.getFullName(), u.getRole()); }
}
