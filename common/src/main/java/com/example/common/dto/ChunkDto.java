package com.example.common.dto;

import java.util.UUID;

public record ChunkDto(
        UUID id,
        UUID documentId,
        UUID userId,
        Integer chunkIndex,
        String content,
        Integer tokenCount
) {}