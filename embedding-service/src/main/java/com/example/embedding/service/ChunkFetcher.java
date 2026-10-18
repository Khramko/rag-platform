package com.example.embedding.service;

import com.example.embedding.dto.ChunkDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChunkFetcher {

    private final WebClient webClient;

    public ChunkFetcher(
            @Value("${document-service.base-url}") String documentServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(documentServiceUrl)
                .build();
    }

    /**
     * Забирает чанки документа из document-service.
     */
    public List<ChunkDto> fetchChunks(UUID documentId, UUID userId) {
        try {
            List<ChunkDto> chunks = webClient.get()
                    .uri("/api/documents/{id}/chunks", documentId)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ChunkDto>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (chunks == null || chunks.isEmpty()) {
                log.warn("No chunks found for document {}", documentId);
                return List.of();
            }
            log.info("Fetched {} chunks for document {}", chunks.size(), documentId);
            return chunks;
        } catch (Exception e) {
            log.error("Failed to fetch chunks for document {}: {}", documentId, e.getMessage());
            throw new RuntimeException("Failed to fetch chunks", e);
        }
    }
}