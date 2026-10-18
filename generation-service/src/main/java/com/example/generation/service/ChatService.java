package com.example.generation.service;

import com.example.generation.dto.OllamaMessage;
import com.example.generation.entity.Chat;
import com.example.generation.entity.Message;
import com.example.generation.repository.ChatRepository;
import com.example.generation.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public Chat getOrCreateChat(UUID userId, String chatId) {
        if (chatId != null && !chatId.isBlank()) {
            UUID id = UUID.fromString(chatId);
            return chatRepository.findByIdAndUserId(id, userId)
                    .orElseGet(() -> createNewChat(userId));
        }
        return createNewChat(userId);
    }

    private Chat createNewChat(UUID userId) {
        Chat chat = Chat.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title("Новый чат")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return chatRepository.save(chat);
    }

    @Transactional
    public Message saveUserMessage(UUID chatId, String content) {
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .role("USER")
                .content(content)
                .createdAt(Instant.now())
                .build();
        return messageRepository.save(msg);
    }

    @Transactional
    public Message saveAssistantMessage(UUID chatId, String content, String sourcesJson) {
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .chatId(chatId)
                .role("ASSISTANT")
                .content(content)
                .sources(sourcesJson)
                .createdAt(Instant.now())
                .build();
        return messageRepository.save(msg);
    }

    @Transactional(readOnly = true)
    public List<OllamaMessage> getHistory(UUID chatId, int limit) {
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        int from = Math.max(0, messages.size() - limit);
        return messages.subList(from, messages.size()).stream()
                .map(m -> new OllamaMessage(m.getRole().toLowerCase(), m.getContent()))
                .toList();
    }

    @Transactional
    public void updateChatTitle(UUID chatId, String title) {
        chatRepository.findById(chatId).ifPresent(chat -> {
            chat.setTitle(title.length() > 100 ? title.substring(0, 100) : title);
            chat.setUpdatedAt(Instant.now());
            chatRepository.save(chat);
        });
    }
}