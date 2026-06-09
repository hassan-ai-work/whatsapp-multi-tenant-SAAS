package com.levosoft.microservice.brain.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.levosoft.microservice.brain.tenant.model.TenantStatus;
import com.levosoft.microservice.brain.tenant.model.TenantPlan;
import com.levosoft.microservice.brain.tenant.model.BillingStatus;
import java.time.OffsetDateTime;

@Schema(description = "Tenant response payload")
public record TenantResponse(
        @Schema(description = "Unique auto-incremented tenant identifier", example = "1")
        Long id,

        @Schema(description = "The unique Keycloak username of the tenant", example = "urpay_admin")
        String username,

        @Schema(description = "The first name of the tenant user", example = "Urpay")
        String firstName,

        @Schema(description = "The last name of the tenant user", example = "SaaS Corporate")
        String lastName,

        @Schema(description = "Current operational status of the tenant", example = "ACTIVE")
        TenantStatus status,

        @Schema(description = "Timestamp when the tenant space was created")
        OffsetDateTime createdAt,

        @Schema(description = "Timestamp when the tenant space was last modified")
        OffsetDateTime updatedAt,

        @Schema(description = "The email of the tenant", example = "contact@acme.com")
        String email,

        @Schema(description = "The plan of the tenant", example = "FREE")
        TenantPlan plan,

        @Schema(description = "The billing status of the tenant", example = "OK")
        BillingStatus billingStatus,

        @Schema(description = "The timezone of the tenant", example = "UTC")
        String timezone
) {
}
