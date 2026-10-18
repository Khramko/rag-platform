package com.example.common.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentUploadedEvent(
        UUID documentId,
        UUID userId,
        String storagePath,
        int chunkCount,
        Instant occurredAt,
        UUID sagaId
) {}