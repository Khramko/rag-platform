package com.example.retrieval.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OllamaEmbedResponse(
        @JsonProperty("embedding") List<Double> embedding
) {}