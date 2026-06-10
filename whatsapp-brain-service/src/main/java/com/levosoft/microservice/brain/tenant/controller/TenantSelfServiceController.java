package com.levosoft.microservice.brain.tenant.controller;

import com.levosoft.microservice.brain.tenant.dto.TenantResponse;
import com.levosoft.microservice.brain.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/tenant")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Self Service", description = "Endpoint for retrieving current tenant data")
public class TenantSelfServiceController {

    private final TenantService tenantService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get current tenant details", description = "Retrieves tenant data for the authenticated user.")
    public TenantResponse getTenant(@RequestHeader(value = "X-Authenticated-User", required = false) String username) {

        if (username == null || username.trim().isEmpty()) {
            log.error("Missing X-Authenticated-User header");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X-Authenticated-User header");
        }

        log.info("Fetching tenant details for authenticated user: {}", username);
        return tenantService.getTenantByUsername(username);
    }
}
