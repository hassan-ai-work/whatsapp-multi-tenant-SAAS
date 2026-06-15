package com.levosoft.microservice.brain.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Contact memory response payload")
public record ContactMemoryResponse(
        @Schema(description = "Unique auto-incremented contact memory identifier", example = "1")
        Long id,

        @Schema(description = "Owning tenant identifier", example = "1")
        Long tenantId,

        @Schema(description = "Owning business identifier", example = "10")
        Long businessId,

        @Schema(description = "User phone number or chat identity", example = "15551234567")
        String contactIdentifier,

        @Schema(description = "Full conversation text restored from stored blob")
        String contactChatText,

        @Schema(description = "Embedding vector values for AI future reference")
        List<Float> embedding,

        @Schema(description = "Deterministic idempotency hash for safe retries")
        String idempotencyHash,

        @Schema(description = "Timestamp when the memory row was created")
        OffsetDateTime createdAt,

        @Schema(description = "Timestamp when the memory row was last modified")
        OffsetDateTime updatedAt
) {
}
