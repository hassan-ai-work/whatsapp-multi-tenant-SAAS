package com.levosoft.microservice.brain.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import com.levosoft.microservice.brain.tenant.model.TenantPlan;
import com.levosoft.microservice.brain.tenant.model.BillingStatus;

public record TenantRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        @Pattern(
                regexp = "^[a-z0-9]+(_[a-z0-9]+)*$",
                message = "username must be lowercase alphanumeric and can only use single underscores as internal separators"
        )
        @Schema(description = "The unique Keycloak username of the tenant", example = "urpay_admin", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank(message = "firstName is required")
        @Size(min = 2, max = 50, message = "firstName must be between 2 and 50 characters")
        @Schema(description = "The first name of the tenant user", example = "Urpay", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @Schema(description = "The last name of the tenant user", example = "SaaS Corporate")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email should be valid")
        @Schema(description = "The email of the tenant", example = "contact@acme.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotNull(message = "plan is required")
        @Schema(description = "The plan of the tenant", example = "FREE", requiredMode = Schema.RequiredMode.REQUIRED)
        TenantPlan plan,

        @NotNull(message = "billingStatus is required")
        @Schema(description = "The billing status of the tenant", example = "OK", requiredMode = Schema.RequiredMode.REQUIRED)
        BillingStatus billingStatus,

        @NotBlank(message = "timezone is required")
        @Schema(description = "The timezone of the tenant", example = "UTC", requiredMode = Schema.RequiredMode.REQUIRED)
        String timezone
) {
}
