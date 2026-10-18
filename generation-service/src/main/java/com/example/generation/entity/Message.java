package com.example.generation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {

    @Id
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(nullable = false)
    private String role;  // USER, ASSISTANT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String sources;  // JSON-строка с источниками

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}