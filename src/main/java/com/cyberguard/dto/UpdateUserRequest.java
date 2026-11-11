package com.cyberguard.dto;

import com.cyberguard.entity.Role;

public record UpdateUserRequest(Role role, Boolean enabled) {}
