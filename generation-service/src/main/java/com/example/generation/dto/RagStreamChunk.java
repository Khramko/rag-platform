package com.example.generation.dto;

import java.util.List;
import java.util.UUID;

public record RagStreamChunk(
        String type,        // "token", "sources", "done", "error"
        String content,     // текст токена (для type=token)
        List<SourceRef> sources,  // источники (для type=sources)
        UUID chatId,        // ID чата (для type=done)
        String error        // сообщение об ошибке (для type=error)
) {
    public record SourceRef(
            UUID chunkId,
            UUID documentId,
            Double similarity,
            String preview
    ) {}

    public static RagStreamChunk token(String t) {
        return new RagStreamChunk("token", t, null, null, null);
    }

    public static RagStreamChunk sources(List<SourceRef> sources) {
        return new RagStreamChunk("sources", null, sources, null, null);
    }

    public static RagStreamChunk done(UUID chatId) {
        return new RagStreamChunk("done", null, null, chatId, null);
    }

    public static RagStreamChunk error(String message) {
        return new RagStreamChunk("error", null, null, null, message);
    }
}