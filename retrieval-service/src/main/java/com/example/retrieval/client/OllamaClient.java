package com.example.retrieval.client;

import com.example.retrieval.dto.OllamaEmbedRequest;
import com.example.retrieval.dto.OllamaEmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class OllamaClient {

    private final WebClient webClient;
    private final String model;
    private final Duration timeout;

    public OllamaClient(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            @Value("${ollama.timeout-seconds:30}") long timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        log.info("Ollama client initialized: baseUrl={}, model={}", baseUrl, model);
    }

    /**
     * Возвращает эмбеддинг для текста.
     */
    public float[] embed(String text) {
        OllamaEmbedRequest request = new OllamaEmbedRequest(model, text);

        OllamaEmbedResponse response = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaEmbedResponse.class)
                .timeout(timeout)
                .block();

        if (response == null || response.embedding() == null) {
            throw new IllegalStateException("Ollama returned empty embedding");
        }

        List<Double> raw = response.embedding();
        float[] result = new float[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            result[i] = raw.get(i).floatValue();
        }
        return result;
    }
}