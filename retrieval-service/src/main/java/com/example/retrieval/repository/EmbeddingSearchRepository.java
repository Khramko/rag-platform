package com.example.retrieval.repository;

import com.example.retrieval.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmbeddingSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<SearchResult> ROW_MAPPER = (rs, rowNum) -> new SearchResult(
            rs.getObject("chunk_id", UUID.class),
            rs.getObject("document_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getString("content"),
            rs.getDouble("similarity")
    );

    public List<SearchResult> searchSimilar(float[] queryVector, UUID userId, int topK) {
        String vectorString = toVectorString(queryVector);

        String sql = """
                SELECT
                    chunk_id,
                    document_id,
                    user_id,
                    content,
                    1 - (embedding <=> ?::vector) AS similarity
                FROM embeddings
                WHERE user_id = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        List<SearchResult> results = jdbcTemplate.query(
                sql, ROW_MAPPER,
                vectorString, userId, vectorString, topK
        );

        log.debug("Found {} results for user {}, topK={}", results.size(), userId, topK);
        return results;
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}