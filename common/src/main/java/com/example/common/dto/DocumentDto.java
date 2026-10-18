package com.example.common.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(
        UUID id,
        UUID userId,
        String filename,
        String contentType,
        Long sizeBytes,
        String status,
        Instant createdAt
) {}