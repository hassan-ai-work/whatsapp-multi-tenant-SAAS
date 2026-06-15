package com.levosoft.microservice.brain.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TenantBusinessRequest(
        @NotNull(message = "tenantId is required")
        @Schema(description = "Owning tenant identifier", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long tenantId,

        @NotBlank(message = "businessName is required")
        @Size(min = 2, max = 150, message = "businessName must be between 2 and 150 characters")
        @Schema(description = "Business display name", example = "Acme Trading LLC", requiredMode = Schema.RequiredMode.REQUIRED)
        String businessName,

        @Schema(description = "Business description", example = "Retail and payment operations for Acme")
        String description,

        @NotBlank(message = "registeredNumber is required")
        @Size(min = 2, max = 100, message = "registeredNumber must be between 2 and 100 characters")
        @Schema(description = "Unique registered business number", example = "CR-2024-000123", requiredMode = Schema.RequiredMode.REQUIRED)
        String registeredNumber
) {
}
