package com.example.generation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OllamaChatChunk(
        @JsonProperty("message") Message message,
        @JsonProperty("done") Boolean done,
        @JsonProperty("error") String error
) {
    public record Message(String role, String content) {}
}