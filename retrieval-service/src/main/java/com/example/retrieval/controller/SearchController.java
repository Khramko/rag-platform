package com.example.retrieval.controller;

import com.example.retrieval.dto.SearchRequest;
import com.example.retrieval.dto.SearchResult;
import com.example.retrieval.service.RetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final RetrievalService retrievalService;

    @PostMapping
    public List<SearchResult> search(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SearchRequest request) {
        return retrievalService.search(request.query(), userId, request.topK());
    }
}