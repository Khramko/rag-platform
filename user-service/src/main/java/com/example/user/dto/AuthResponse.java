package com.example.user.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        long expiresInSeconds,
        UserDto user
) {
    public record UserDto(
            UUID id,
            String email,
            String displayName,
            String role
    ) {}
}