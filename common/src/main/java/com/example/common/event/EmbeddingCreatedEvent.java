package com.example.common.event;

import java.time.Instant;
import java.util.UUID;

public record EmbeddingCreatedEvent(
        UUID documentId,
        UUID userId,
        int embeddingCount,
        Instant occurredAt,
        UUID sagaId
) {}