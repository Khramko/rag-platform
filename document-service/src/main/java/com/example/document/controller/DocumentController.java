package com.example.document.controller;

import com.example.common.dto.ChunkDto;
import com.example.common.dto.DocumentDto;
import com.example.document.entity.Chunk;
import com.example.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto upload(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("file") MultipartFile file) {
        return documentService.upload(userId, file);
    }

    @GetMapping
    public List<DocumentDto> list(@RequestHeader("X-User-Id") UUID userId) {
        return documentService.listByUser(userId);
    }

    @GetMapping("/{id}")
    public DocumentDto get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return documentService.get(userId, id);
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        documentService.delete(userId, id);
    }

    @GetMapping("/{id}/chunks")
    public List<ChunkDto> chunks(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return documentService.getChunks(userId, id).stream()
                .map(c -> new ChunkDto(
                        c.getId(),
                        c.getDocumentId(),
                        userId,
                        c.getChunkIndex(),
                        c.getContent(),
                        c.getTokenCount()))
                .toList();
    }
}