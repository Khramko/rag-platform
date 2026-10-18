package com.example.generation.service;

import com.example.generation.client.OllamaChatClient;
import com.example.generation.client.RetrievalClient;
import com.example.generation.dto.OllamaMessage;
import com.example.generation.dto.RagStreamChunk;
import com.example.generation.dto.SearchResult;
import com.example.generation.entity.Chat;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
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

    /**
     * Основной RAG-пайплайн. Возвращает Flux SSE-событий.
     */
    public Flux<RagStreamChunk> query(UUID userId, String query, String chatId) {
        Chat chat = chatService.getOrCreateChat(userId, chatId);
        UUID finalChatId = chat.getId();
        chatService.saveUserMessage(finalChatId, query);

        return retrievalClient.search(query, userId)
                .flatMapMany(context -> {
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

                    Flux<RagStreamChunk> tokenStream = ollamaChatClient.streamChat(prompt)
                            .doOnNext(token -> answerBuffer.get().append(token))
                            .map(RagStreamChunk::token);

                    Flux<RagStreamChunk> doneStream = Flux.defer(() -> {
                        String answer = answerBuffer.get().toString();
                        String sourcesJson = toJson(sources);
                        chatService.saveAssistantMessage(finalChatId, answer, sourcesJson);
                        chatService.updateChatTitle(finalChatId, query);
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