package com.example.generation.service;

import com.example.generation.client.OllamaChatClient;
import com.example.generation.client.RetrievalClient;
import com.example.generation.dto.OllamaMessage;
import com.example.generation.dto.RagStreamChunk;
import com.example.generation.dto.SearchResult;
import com.example.generation.entity.Chat;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipeline {

    private final RetrievalClient retrievalClient;
    private final OllamaChatClient ollamaChatClient;
    private final PromptBuilder promptBuilder;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private Timer retrievalTimer;
    private Timer totalTimer;
    private Timer ttftTimer;          // time to first token
    private Timer generationTimer;    // от первого токена до последнего

    @PostConstruct
    void initTimers() {
        this.retrievalTimer = Timer.builder("rag.generation.retrieval.duration")
                .description("Time to retrieve top-K chunks from retrieval-service")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.totalTimer = Timer.builder("rag.generation.total.duration")
                .description("Total RAG query time (retrieval + generation)")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.ttftTimer = Timer.builder("rag.generation.ttft.duration")
                .description("Time to first token (TTFT) from request start")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.generationTimer = Timer.builder("rag.generation.stream.duration")
                .description("Time from first token to last token")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /**
     * Основной RAG-пайплайн. Возвращает Flux SSE-событий.
     *
     * Метрики (Prometheus):
     *  - rag_generation_retrieval_duration_seconds   — время retrieval
     *  - rag_generation_ttft_duration_seconds        — time to first token
     *  - rag_generation_stream_duration_seconds      — время стриминга (first→last)
     *  - rag_generation_total_duration_seconds       — total (start→done)
     */
    public Flux<RagStreamChunk> query(UUID userId, String query, String chatId) {
        // 1. Создание чата и сохранение user message — блокирующие JDBC,
        //    выполняем ДО возврата Flux (как в оригинале), но замеряем в totalTimer.
        Timer.Sample totalSample = Timer.start(meterRegistry);
        long startNanos = System.nanoTime();

        Chat chat = chatService.getOrCreateChat(userId, chatId);
        UUID finalChatId = chat.getId();
        chatService.saveUserMessage(finalChatId, query);

        // 2. Retrieval с таймером
        Timer.Sample retrievalSample = Timer.start(meterRegistry);

        return retrievalClient.search(query, userId)
                .flatMapMany(context -> {
                    retrievalSample.stop(retrievalTimer);
                    log.info("Retrieved {} chunks for query '{}'", context.size(), query);

                    List<RagStreamChunk.SourceRef> sources = context.stream()
                            .map(r -> new RagStreamChunk.SourceRef(
                                    r.chunkId(), r.documentId(), r.similarity(),
                                    truncate(r.content(), 200)))
                            .toList();

                    List<OllamaMessage> history = chatService.getHistory(finalChatId, 10);
                    List<OllamaMessage> prompt = promptBuilder.build(history, query, context);

                    AtomicReference<StringBuilder> answerBuffer =
                            new AtomicReference<>(new StringBuilder());
                    AtomicBoolean firstToken = new AtomicBoolean(false);
                    AtomicLong firstTokenNanos = new AtomicLong(0);

                    Flux<RagStreamChunk> tokenStream = ollamaChatClient.streamChat(prompt)
                            .doOnNext(token -> {
                                // TTFT: первый токен
                                if (firstToken.compareAndSet(false, true)) {
                                    long ttftNanos = System.nanoTime() - startNanos;
                                    ttftTimer.record(ttftNanos, TimeUnit.NANOSECONDS);
                                    firstTokenNanos.set(System.nanoTime());
                                }
                                answerBuffer.get().append(token);
                            })
                            .map(RagStreamChunk::token);

                    Flux<RagStreamChunk> doneStream = Flux.defer(() -> {
                        String answer = answerBuffer.get().toString();
                        String sourcesJson = toJson(sources);
                        chatService.saveAssistantMessage(finalChatId, answer, sourcesJson);
                        chatService.updateChatTitle(finalChatId, query);

                        // stream: от первого токена до последнего
                        if (firstTokenNanos.get() > 0) {
                            long streamNanos = System.nanoTime() - firstTokenNanos.get();
                            generationTimer.record(streamNanos, TimeUnit.NANOSECONDS);
                        }

                        log.info("RAG pipeline done for chat {}, answer length={}",
                                finalChatId, answer.length());
                        return Flux.just(RagStreamChunk.done(finalChatId));
                    });

                    return Flux.concat(
                            Flux.just(RagStreamChunk.sources(sources)),
                            tokenStream,
                            doneStream
                    );
                })
                .doFinally(signal -> {
                    // total: всегда, даже при ошибке/отмене
                    totalSample.stop(totalTimer);
                    if (signal == SignalType.CANCEL) {
                        log.warn("RAG pipeline cancelled for chat {}", finalChatId);
                    }
                })
                .onErrorResume(e -> {
                    log.error("RAG pipeline error", e);
                    return Flux.just(RagStreamChunk.error(e.getMessage()));
                });
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}