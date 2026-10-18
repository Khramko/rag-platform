package com.example.document.repository;

import com.example.document.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {
    List<Chunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
    void deleteByDocumentId(UUID documentId);
}