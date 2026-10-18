package com.example.generation.client;

import com.example.generation.dto.OllamaChatChunk;
import com.example.generation.dto.OllamaChatRequest;
import com.example.generation.dto.OllamaMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class OllamaChatClient {

    private final WebClient webClient;
    private final String model;
    private final Duration timeout;
    private final Double temperature;
    private final Integer maxTokens;
    private final ObjectMapper objectMapper;

    public OllamaChatClient(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.chat-model}") String model,
            @Value("${ollama.timeout-seconds:300}") long timeoutSeconds,
            @Value("${ollama.temperature:0.7}") Double temperature,
            @Value("${ollama.max-tokens:2048}") Integer maxTokens,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
        log.info("Ollama chat client initialized: model={}, timeout={}s", model, timeoutSeconds);
    }

    /**
     * Стримит ответ от Ollama как Flux<String> (токены).
     */
    public Flux<String> streamChat(List<OllamaMessage> messages) {
        OllamaChatRequest request = new OllamaChatRequest(
                model,
                messages,
                true,
                new OllamaChatRequest.Options(temperature, maxTokens)
        );

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                // Ollama шлёт NDJSON: каждая строка — отдельный JSON
                .flatMap(this::parseChunk)
                .filter(s -> !s.isEmpty())
                .timeout(timeout)
                .doOnError(e -> log.error("Ollama stream error: {}", e.getMessage()));
    }

    /**
     * Парсит один NDJSON-чанк и возвращает текст токена.
     */
    private Flux<String> parseChunk(String json) {
        try {
            OllamaChatChunk chunk = objectMapper.readValue(json, OllamaChatChunk.class);
            if (chunk.error() != null) {
                log.warn("Ollama chunk error: {}", chunk.error());
                return Flux.empty();
            }
            if (chunk.message() != null && chunk.message().content() != null) {
                return Flux.just(chunk.message().content());
            }
            return Flux.empty();
        } catch (Exception e) {
            log.warn("Failed to parse Ollama chunk: {}", json);
            return Flux.empty();
        }
    }
}