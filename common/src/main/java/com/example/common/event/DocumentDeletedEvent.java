package com.example.common.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentDeletedEvent(
        UUID documentId,
        UUID userId,
        Instant occurredAt
) {}