package com.levosoft.microservice.brain.tenant.dto;

import com.levosoft.microservice.brain.tenant.model.TenantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Tenant response payload")
public record TenantResponse(
        @Schema(description = "Unique auto-incremented tenant identifier", example = "1")
        Long id,

        @Schema(description = "The unique name of the tenant", example = "Acme Corporation")
        String name,

        @Schema(description = "Current operational status of the tenant", example = "ACTIVE")
        TenantStatus status,

        @Schema(description = "Timestamp when the tenant space was created")
        OffsetDateTime createdAt,

        @Schema(description = "Timestamp when the tenant space was last modified")
        OffsetDateTime updatedAt
) {
}
