package com.example.embedding.listener;

import com.example.common.event.DocumentUploadedEvent;
import com.example.embedding.service.EmbeddingProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentUploadedListener {

    private final EmbeddingProcessor embeddingProcessor;

    @KafkaListener(
            topics = "${kafka.topics.document-uploaded}",
            groupId = "embedding-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        log.info("Received document.uploaded: documentId={}, userId={}, chunks={}, sagaId={}",
                event.documentId(), event.userId(), event.chunkCount(), event.sagaId());

        embeddingProcessor.processDocument(
                event.documentId(), event.userId(), event.sagaId());
    }
}