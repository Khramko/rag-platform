package com.example.generation.dto;

import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(
        @NotBlank(message = "query must not be blank")
        String query,

        // опционально — продолжить существующий чат
        String chatId
) {}