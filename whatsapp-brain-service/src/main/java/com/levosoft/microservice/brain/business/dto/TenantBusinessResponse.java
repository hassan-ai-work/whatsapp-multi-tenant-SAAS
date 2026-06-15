package com.levosoft.microservice.brain.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Tenant business response payload")
public record TenantBusinessResponse(
        @Schema(description = "Unique auto-incremented business identifier", example = "1")
        Long id,

        @Schema(description = "Owning tenant identifier", example = "1")
        Long tenantId,

        @Schema(description = "Business display name", example = "Acme Trading LLC")
        String businessName,

        @Schema(description = "Business description", example = "Retail and payment operations for Acme")
        String description,

        @Schema(description = "Unique registered business number", example = "CR-2024-000123")
        String registeredNumber,

        @Schema(description = "Timestamp when the business row was created")
        OffsetDateTime createdAt,

        @Schema(description = "Timestamp when the business row was last modified")
        OffsetDateTime updatedAt
) {
}
