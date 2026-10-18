package com.example.generation.controller;

import com.example.generation.dto.RagQueryRequest;
import com.example.generation.dto.RagStreamChunk;
import com.example.generation.service.RagPipeline;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagPipeline ragPipeline;

    @PostMapping(value = "/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<RagStreamChunk> query(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RagQueryRequest request) {
        log.info("RAG query from user {}: '{}'", userId, request.query());
        return ragPipeline.query(userId, request.query(), request.chatId());
    }
}