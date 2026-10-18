package com.example.generation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        @JsonProperty("options") Options options
) {
    public record Options(
            Double temperature,
            @JsonProperty("num_predict") Integer numPredict
    ) {}
}