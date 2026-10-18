package com.example.common.event;

import java.time.Instant;
import java.util.UUID;

public record EmbeddingFailedEvent(
        UUID documentId,
        UUID userId,
        String reason,
        Instant occurredAt,
        UUID sagaId
) {}