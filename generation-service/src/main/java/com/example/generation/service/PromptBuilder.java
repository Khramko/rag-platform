package com.example.generation.service;

import com.example.generation.dto.OllamaMessage;
import com.example.generation.dto.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PromptBuilder {

    @Value("${rag.max-context-chars:4000}")
    private int maxContextChars;

    private static final String SYSTEM_PROMPT = """
            Ты — ассистент, который отвечает на вопросы пользователя,
            используя ТОЛЬКО предоставленный контекст из его документов.

            Правила:
            1. Если в контексте есть ответ — отвечай на его основе.
            2. Если ответа в контексте нет — честно скажи "В предоставленных документах нет информации по этому вопросу".
            3. Не выдумывай факты, не используй внешние знания.
            4. Отвечай на языке вопроса.
            5. Будь кратким и по делу.
            """;

    /**
     * Строит список сообщений для LLM.
     */
    public List<OllamaMessage> build(List<OllamaMessage> history, String query, List<SearchResult> context) {
        List<OllamaMessage> messages = new ArrayList<>();

        // 1. System prompt
        messages.add(new OllamaMessage("system", SYSTEM_PROMPT));

        // 2. История (последние N сообщений)
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }

        // 3. Контекст + вопрос
        String userMessage = buildUserMessage(query, context);
        messages.add(new OllamaMessage("user", userMessage));

        return messages;
    }

    private String buildUserMessage(String query, List<SearchResult> context) {
        if (context == null || context.isEmpty()) {
            return "Контекст отсутствует.\n\nВопрос: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Контекст из документов пользователя:\n\n");

        int used = 0;
        for (int i = 0; i < context.size(); i++) {
            SearchResult r = context.get(i);
            String block = String.format(
                    "--- Источник %d (similarity=%.3f) ---\n%s\n\n",
                    i + 1, r.similarity(), r.content()
            );
            if (used + block.length() > maxContextChars) {
                log.debug("Context truncated at source {}", i);
                break;
            }
            sb.append(block);
            used += block.length();
        }

        sb.append("Вопрос: ").append(query);
        return sb.toString();
    }
}