package com.example.document.service;

import com.example.common.dto.DocumentDto;
import com.example.common.event.DocumentDeletedEvent;
import com.example.common.event.DocumentUploadedEvent;
import com.example.document.entity.Chunk;
import com.example.document.entity.Document;
import com.example.document.entity.DocumentStatus;
import com.example.document.exception.DocumentNotFoundException;
import com.example.document.repository.ChunkRepository;
import com.example.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final StorageService storageService;
    private final ChunkingService chunkingService;
    private final DocumentEventPublisher eventPublisher;

    @Transactional
    public DocumentDto upload(UUID userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        UUID sagaId = UUID.randomUUID();          // ← НОВОЕ
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();

        Document document = Document.builder()
                .id(documentId)
                .userId(userId)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .status(DocumentStatus.UPLOADED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        documentRepository.save(document);

        try {
            // 1. Загрузить в MinIO
            String storagePath = storageService.upload(userId, documentId, file);
            document.setStoragePath(storagePath);

            // 2. Извлечь текст и нарезать чанки
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            List<String> chunks;
            try (InputStream is = file.getInputStream()) {
                chunks = chunkingService.extractAndChunk(is, file.getOriginalFilename());
            }

            // 3. Сохранить чанки
            int index = 0;
            for (String content : chunks) {
                Chunk chunk = Chunk.builder()
                        .id(UUID.randomUUID())
                        .documentId(documentId)
                        .chunkIndex(index++)
                        .content(content)
                        .tokenCount(chunkingService.estimateTokens(content))
                        .createdAt(Instant.now())
                        .build();
                chunkRepository.save(chunk);
            }

            // 4. Опубликовать событие — с sagaId
            eventPublisher.publishUploaded(new DocumentUploadedEvent(
                    documentId,
                    userId,
                    storagePath,
                    chunks.size(),
                    Instant.now(),
                    sagaId                       // ← НОВОЕ
            ));

            // ВАЖНО: не ставим READY, оставляем PROCESSING
            // Статус READY поставит EmbeddingResultListener после embedding.created

            log.info("Document {} uploaded, status=PROCESSING, chunks={}, sagaId={}",
                    documentId, chunks.size(), sagaId);
            return toDto(document);

        } catch (Exception e) {
            log.error("Failed to upload document {}", documentId, e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listByUser(UUID userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDto get(UUID userId, UUID documentId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return toDto(document);
    }

    @Transactional(readOnly = true)
    public List<Chunk> getChunks(UUID userId, UUID documentId) {
        // Проверка владения
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
    }

    @Transactional
    public void delete(UUID userId, UUID documentId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        // Удалить из MinIO
        if (document.getStoragePath() != null) {
            storageService.delete(document.getStoragePath());
        }

        // Удалить чанки и документ
        chunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);

        // Опубликовать событие
        eventPublisher.publishDeleted(new DocumentDeletedEvent(
                documentId, userId, Instant.now()
        ));

        log.info("Deleted document {}", documentId);
    }

    private DocumentDto toDto(Document d) {
        return new DocumentDto(
                d.getId(),
                d.getUserId(),
                d.getFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getStatus().name(),
                d.getCreatedAt()
        );
    }
}