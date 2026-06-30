package com.levosoft.microservice.chat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.levosoft.microservice.chat.config.ChatProperties;
import com.levosoft.microservice.chat.model.DocumentContextChunk;

@Service
public class JdbcVectorStoreLookup implements VectorStoreLookup {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ChatProperties chatProperties;

    public JdbcVectorStoreLookup(NamedParameterJdbcTemplate jdbcTemplate,
                                 ChatProperties chatProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatProperties = chatProperties;
    }

    @Override
    public List<DocumentContextChunk> lookup(String tenantIdentity, float[] queryVector, int limit) {

        String sql = """
                SELECT content,
                       metadata::text AS metadata_json,
                       1 - (embedding <=> CAST(:query_vector AS vector)) AS score
                FROM document_chunks
                WHERE metadata->>'tenant_identity' = :tenant_identity
                ORDER BY embedding <=> CAST(:query_vector AS vector)
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenant_identity", tenantIdentity)
                .addValue("query_vector", toVectorLiteral(queryVector))
                .addValue("limit", limit <= 0 ? chatProperties.rag().maxResults() : limit);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        List<DocumentContextChunk> chunks = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            double score = row.get("score") == null ? 0.0d : ((Number) row.get("score")).doubleValue();
            String metadataJson = row.get("metadata_json") == null ? "{}" : String.valueOf(row.get("metadata_json"));
            chunks.add(new DocumentContextChunk(String.valueOf(row.get("content")), score, metadataJson));
        }
        return chunks;
    }

    private String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}

