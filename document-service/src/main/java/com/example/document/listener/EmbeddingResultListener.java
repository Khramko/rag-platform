package com.example.document.listener;

import com.example.common.event.EmbeddingCreatedEvent;
import com.example.common.event.EmbeddingFailedEvent;
import com.example.document.entity.Document;
import com.example.document.entity.DocumentStatus;
import com.example.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingResultListener {

    private final DocumentRepository documentRepository;

    @KafkaListener(
            topics = "embedding.created",
            groupId = "document-service-embedding"
    )
    public void onEmbeddingCreated(EmbeddingCreatedEvent event) {
        log.info("Received embedding.created: documentId={}, count={}, sagaId={}",
                event.documentId(), event.embeddingCount(), event.sagaId());

        documentRepository.findById(event.documentId()).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.READY);
            doc.setUpdatedAt(Instant.now());
            documentRepository.save(doc);
            log.info("Document {} → READY", event.documentId());
        });
    }

    @KafkaListener(
            topics = "embedding.failed",
            groupId = "document-service-embedding"
    )
    public void onEmbeddingFailed(EmbeddingFailedEvent event) {
        log.error("Received embedding.failed: documentId={}, reason={}, sagaId={}",
                event.documentId(), event.reason(), event.sagaId());

        documentRepository.findById(event.documentId()).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage("Embedding failed: " + event.reason());
            doc.setUpdatedAt(Instant.now());
            documentRepository.save(doc);
            log.info("Document {} → FAILED", event.documentId());
        });
    }
}