package com.example.retrieval.service;

import com.example.retrieval.client.OllamaClient;
import com.example.retrieval.dto.SearchResult;
import com.example.retrieval.repository.EmbeddingSearchRepository;
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

    @Value("${retrieval.default-top-k:5}")
    private int defaultTopK;

    @Value("${retrieval.max-top-k:50}")
    private int maxTopK;

    /**
     * Ищет top-K чанков, похожих на запрос.
     */
    public List<SearchResult> search(String query, UUID userId, Integer topK) {
        int k = topK == null ? defaultTopK : Math.min(topK, maxTopK);

        long start = System.currentTimeMillis();

        // 1. Эмбеддинг запроса
        float[] queryVector = ollamaClient.embed(query);
        long embedTime = System.currentTimeMillis() - start;

        // 2. Векторный поиск
        List<SearchResult> results = searchRepository.searchSimilar(queryVector, userId, k);
        long totalTime = System.currentTimeMillis() - start;

        log.info("Search done: query='{}', userId={}, topK={}, results={}, embedTime={}ms, totalTime={}ms",
                truncate(query, 50), userId, k, results.size(), embedTime, totalTime);

        return results;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}