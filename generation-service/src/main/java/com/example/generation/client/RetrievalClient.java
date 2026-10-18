package com.example.generation.client;

import com.example.generation.dto.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class RetrievalClient {

    private final WebClient webClient;
    private final int topK;

    public RetrievalClient(
            @Value("${retrieval-service.base-url}") String baseUrl,
            @Value("${retrieval-service.top-k:5}") int topK) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.topK = topK;
    }

    /** Реактивный поиск — НЕ блокирующий. */
    public Mono<List<SearchResult>> search(String query, UUID userId) {
        return webClient.post()
                .uri("/api/search")
                .header("X-User-Id", userId.toString())
                .bodyValue(Map.of("query", query, "topK", topK))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<SearchResult>>() {})
                .timeout(Duration.ofSeconds(30))
                .doOnError(e -> log.error("Retrieval failed: {}", e.getMessage()))
                .onErrorReturn(List.of());   // fallback: пустой список
    }
}