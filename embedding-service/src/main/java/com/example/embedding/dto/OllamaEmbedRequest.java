package com.example.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OllamaEmbedRequest(
        @JsonProperty("model") String model,
        @JsonProperty("prompt") String prompt
) {}