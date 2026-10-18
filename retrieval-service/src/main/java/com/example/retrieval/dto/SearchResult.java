package com.example.retrieval.dto;

import java.util.UUID;

public record SearchResult(
        UUID chunkId,
        UUID documentId,
        UUID userId,
        String content,
        Double similarity
) {}