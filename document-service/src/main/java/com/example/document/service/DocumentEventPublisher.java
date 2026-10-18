package com.example.document.service;

import com.example.common.event.DocumentDeletedEvent;
import com.example.common.event.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.document-uploaded}")
    private String documentUploadedTopic;

    @Value("${kafka.topics.document-deleted}")
    private String documentDeletedTopic;

    public void publishUploaded(DocumentUploadedEvent event) {
        kafkaTemplate.send(documentUploadedTopic, event.documentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish document.uploaded for {}", event.documentId(), ex);
                    } else {
                        log.info("Published document.uploaded for {}", event.documentId());
                    }
                });
    }

    public void publishDeleted(DocumentDeletedEvent event) {
        kafkaTemplate.send(documentDeletedTopic, event.documentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish document.deleted for {}", event.documentId(), ex);
                    } else {
                        log.info("Published document.deleted for {}", event.documentId());
                    }
                });
    }
}