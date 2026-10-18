package com.example.embedding.dto;

import com.example.embedding.dto.OllamaEmbedRequest;
import com.example.embedding.dto.OllamaEmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class OllamaClient {

    private final WebClient webClient;
    private final String model;

    public OllamaClient(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
        log.info("Ollama client initialized: baseUrl={}, model={}", baseUrl, model);
    }

    /**
     * Возвращает эмбеддинг для одного текста.
     */
    public float[] embed(String text) {
        OllamaEmbedRequest request = new OllamaEmbedRequest(model, text);

        OllamaEmbedResponse response = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaEmbedResponse.class)
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> {
                    log.error("Ollama embed failed: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();

        if (response == null || response.embedding() == null) {
            throw new RuntimeException("Ollama returned empty embedding");
        }

        List<Double> raw = response.embedding();
        float[] result = new float[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            result[i] = raw.get(i).floatValue();
        }
        return result;
    }

    /**
     * Проверка доступности Ollama.
     */
    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}