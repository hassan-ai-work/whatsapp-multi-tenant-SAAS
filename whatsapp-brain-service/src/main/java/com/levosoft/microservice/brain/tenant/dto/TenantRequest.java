package com.levosoft.microservice.brain.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Tenant creation request payload")
public record TenantRequest(
        @NotBlank(message = "name is required")
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        @Schema(description = "The unique name of the tenant", example = "Acme Corporation", requiredMode = Schema.RequiredMode.REQUIRED)
        String name
) {
}
