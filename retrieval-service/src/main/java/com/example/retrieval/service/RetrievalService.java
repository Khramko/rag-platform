package com.example.retrieval.service;

import com.example.retrieval.client.OllamaClient;
import com.example.retrieval.dto.SearchResult;
import com.example.retrieval.repository.EmbeddingSearchRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final OllamaClient ollamaClient;
    private final EmbeddingSearchRepository searchRepository;
    private final MeterRegistry meterRegistry;   // ← добавлено

    @Value("${retrieval.default-top-k:10}")
    private int defaultTopK;

    @Value("${retrieval.max-top-k:50}")
    private int maxTopK;

    @Value("${ollama.model}")
    private String model;                        // ← добавлено

    private Timer queryEmbeddingTimer;
    private Timer searchTimer;

    @PostConstruct
    void initTimers() {
        this.queryEmbeddingTimer = Timer.builder("rag.retrieval.query_embedding.duration")
                .description("Time to embed the user query via Ollama")
                .tag("model", model)
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.searchTimer = Timer.builder("rag.retrieval.search.duration")
                .description("Time to run vector search in pgvector")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public List<SearchResult> search(String query, UUID userId, Integer topK) {
        int k = topK == null ? defaultTopK : Math.min(topK, maxTopK);

        float[] queryVector = queryEmbeddingTimer.record(() ->
                ollamaClient.embed(query));

        return searchTimer.record(() ->
                searchRepository.searchSimilar(queryVector, userId, k));
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}