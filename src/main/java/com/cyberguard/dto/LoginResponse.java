package com.cyberguard.dto;

public record LoginResponse(String token, long expiresInMinutes, UserDto user) {}
