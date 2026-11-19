package com.example.embedding.service;

import com.example.common.event.EmbeddingCreatedEvent;
import com.example.common.event.EmbeddingFailedEvent;
import com.example.embedding.dto.ChunkDto;
import com.example.embedding.client.OllamaClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingProcessor {

    private final ChunkFetcher chunkFetcher;
    private final OllamaClient ollamaClient;
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${ollama.model}")
    private String model;

    private Timer embeddingTimer;
    private Timer documentTimer;

    @PostConstruct
    void initTimers() {
        this.embeddingTimer = Timer.builder("rag.embedding.duration")
                .description("Time to compute one embedding via Ollama")
                .tag("model", model)
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.documentTimer = Timer.builder("rag.document.embedding.duration")
                .description("Time to embed a whole document")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void processDocument(UUID documentId, UUID userId, UUID sagaId) {
        documentTimer.record(() -> doProcessDocument(documentId, userId, sagaId));
    }

    private void doProcessDocument(UUID documentId, UUID userId, UUID sagaId) {
        try {
            List<ChunkDto> chunks = chunkFetcher.fetchChunks(documentId, userId);
            if (chunks.isEmpty()) {
                throw new RuntimeException("No chunks found for document " + documentId);
            }

            int saved = 0;
            for (ChunkDto chunk : chunks) {
                if (embeddingExists(chunk.id())) continue;

                // Timer.record(Supplier) — возвращает результат лямбды.
                // Важно: Runnable потерял бы float[] (был бы баг).
                float[] vector = embeddingTimer.record(() ->
                        ollamaClient.embed(chunk.content()));

                saveEmbedding(chunk, userId, vector);
                saved++;
            }

            log.info("Processed document {}: {}/{} chunks embedded",
                    documentId, saved, chunks.size());

            // УСПЕХ — публикуем событие
            kafkaTemplate.send("embedding.created", new EmbeddingCreatedEvent(
                    documentId, userId, saved, Instant.now(), sagaId
            ));
            log.info("Published embedding.created for document {}, sagaId={}",
                    documentId, sagaId);

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);

            // ОШИБКА — публикуем событие
            kafkaTemplate.send("embedding.failed", new EmbeddingFailedEvent(
                    documentId, userId, e.getMessage(), Instant.now(), sagaId
            ));
            log.info("Published embedding.failed for document {}, sagaId={}",
                    documentId, sagaId);
        }
    }

    private boolean embeddingExists(UUID chunkId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM embeddings WHERE chunk_id = ?",
                Integer.class, chunkId);
        return count != null && count > 0;
    }

    private void saveEmbedding(ChunkDto chunk, UUID userId, float[] vector) {
        String vectorString = toVectorString(vector);
        jdbcTemplate.update(
                "INSERT INTO embeddings (id, chunk_id, document_id, user_id, content, embedding, model, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?::vector, ?, ?)",
                UUID.randomUUID(),
                chunk.id(),
                chunk.documentId(),
                userId,
                chunk.content(),
                vectorString,
                model,
                Timestamp.from(Instant.now())
        );
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}