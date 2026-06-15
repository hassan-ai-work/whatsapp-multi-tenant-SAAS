package com.levosoft.microservice.brain.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ContactMemoryRequest(
        @NotBlank(message = "contactIdentifier is required")
        @Size(min = 2, max = 255, message = "contactIdentifier must be between 2 and 255 characters")
        @Schema(description = "User phone number or chat identity", example = "15551234567", requiredMode = Schema.RequiredMode.REQUIRED)
        String contactIdentifier,

        @NotBlank(message = "contactChatText is required")
        @Schema(description = "Full conversation text to be stored as bytes", example = "Customer asked about pricing and delivery.")
        String contactChatText,

        @NotNull(message = "embedding is required")
        @Schema(description = "Embedding vector values for AI future reference")
        List<Double> embedding,

        @NotBlank(message = "idempotencyHash is required")
        @Size(min = 8, max = 255, message = "idempotencyHash must be between 8 and 255 characters")
        @Schema(description = "Deterministic idempotency hash for safe retries", example = "7d4c0f1c7fca8b52e1a6bcb1e6f4d4c1", requiredMode = Schema.RequiredMode.REQUIRED)
        String idempotencyHash
) {
}
