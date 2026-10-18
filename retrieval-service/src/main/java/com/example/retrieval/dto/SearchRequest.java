package com.example.retrieval.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank(message = "query must not be blank")
        String query,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 50, message = "topK must be at most 50")
        Integer topK
) {
    public int topKOrDefault(int defaultValue) {
        return topK == null ? defaultValue : topK;
    }
}