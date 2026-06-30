package com.levosoft.microservice.chat.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ContactMemoryArchiveService {

    private final TenantIdentityResolver tenantIdentityResolver;
    private final EmbeddingGateway embeddingGateway;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ContactMemoryArchiveService(TenantIdentityResolver tenantIdentityResolver,
                                       EmbeddingGateway embeddingGateway,
                                       NamedParameterJdbcTemplate jdbcTemplate,
                                       ObjectMapper objectMapper) {
        this.tenantIdentityResolver = tenantIdentityResolver;
        this.embeddingGateway = embeddingGateway;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void archive(String tenantIdentity, String customerIdentity, String transcript, String sessionKey) {
        if (!StringUtils.hasText(transcript)) {
            return;
        }
        TenantIdentityResolver.ResolvedIdentity identity = tenantIdentityResolver.resolve(tenantIdentity)
                .orElseThrow(() -> new IllegalStateException("No business row found for tenantIdentity " + tenantIdentity));

        String chatTextJson = toChatTextJson(tenantIdentity, customerIdentity, transcript, sessionKey);
        float[] embedding = embeddingGateway.embed(transcript);
        String idempotencyHash = hash(sessionKey + "|" + transcript);

        String sql = """
                INSERT INTO contact_memories (
                    tenant_id,
                    business_id,
                    contact_identifier,
                    contact_chat_text,
                    embedding,
                    idempotency_hash
                ) VALUES (
                    :tenant_id,
                    :business_id,
                    :contact_identifier,
                    CAST(:contact_chat_text AS jsonb),
                    CAST(:embedding AS vector),
                    :idempotency_hash
                )
                ON CONFLICT (tenant_id, business_id, contact_identifier, idempotency_hash)
                DO UPDATE SET
                    contact_chat_text = EXCLUDED.contact_chat_text,
                    embedding = EXCLUDED.embedding,
                    updated_at = NOW()
                """;

        jdbcTemplate.update(sql, Map.of(
                "tenant_id", identity.tenantId(),
                "business_id", identity.businessId(),
                "contact_identifier", customerIdentity,
                "contact_chat_text", chatTextJson,
                "embedding", toVectorLiteral(embedding),
                "idempotency_hash", idempotencyHash));
    }

    private String toChatTextJson(String tenantIdentity, String customerIdentity, String transcript, String sessionKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantIdentity", tenantIdentity);
        payload.put("customerIdentity", customerIdentity);
        payload.put("sessionKey", sessionKey);
        payload.put("archivedAt", Instant.now().toString());
        payload.put("transcript", transcript);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize transcript payload", ex);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
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

