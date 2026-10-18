package com.example.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChunkingService {

    private final Tika tika = new Tika();

    @Value("${chunking.max-tokens:500}")
    private int maxTokens;

    @Value("${chunking.overlap-tokens:50}")
    private int overlapTokens;

    /**
     * Извлекает текст из файла и разбивает на чанки.
     */
    public List<String> extractAndChunk(InputStream inputStream, String filename) {
        try {
            String text = tika.parseToString(inputStream);
            log.debug("Extracted {} chars from {}", text.length(), filename);
            return chunk(text);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from " + filename, e);
        }
    }

    /**
     * Простой чанкер: разбивает по абзацам, склеивает до maxTokens.
     */
    private List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // Нормализация
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\n{3,}", "\n\n").trim();

        // Разбить на абзацы
        String[] paragraphs = text.split("\n\n+");

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;

        for (String paragraph : paragraphs) {
            int paragraphTokens = estimateTokens(paragraph);

            // Если абзац сам больше maxTokens — режем его
            if (paragraphTokens > maxTokens) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                    currentTokens = 0;
                }
                chunks.addAll(splitLongParagraph(paragraph));
                continue;
            }

            // Если добавление превысит лимит — сохранить текущий чанк
            if (currentTokens + paragraphTokens > maxTokens && current.length() > 0) {
                chunks.add(current.toString().trim());

                // Overlap: взять хвост предыдущего
                String tail = tailByTokens(current.toString(), overlapTokens);
                current.setLength(0);
                current.append(tail);
                currentTokens = estimateTokens(tail);
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph);
            currentTokens += paragraphTokens;
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        log.debug("Split into {} chunks", chunks.size());
        return chunks;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> result = new ArrayList<>();
        // Простое деление по предложениям
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;

        for (String sentence : sentences) {
            int sentenceTokens = estimateTokens(sentence);
            if (currentTokens + sentenceTokens > maxTokens && current.length() > 0) {
                result.add(current.toString().trim());
                current.setLength(0);
                currentTokens = 0;
            }
            if (current.length() > 0) current.append(" ");
            current.append(sentence);
            currentTokens += sentenceTokens;
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private String tailByTokens(String text, int tokens) {
        int chars = tokens * 4;
        return text.length() <= chars ? text : text.substring(text.length() - chars);
    }

    /**
     * Очень грубая оценка токенов: 1 токен ≈ 4 символа.
     */
    public int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 4;
    }
}