package com.example.generation.dto;

import java.util.UUID;

public record SearchResult(
        UUID chunkId,
        UUID documentId,
        UUID userId,
        String content,
        Double similarity
) {}