package com.levosoft.microservice.brain.tenant.controller;

import com.levosoft.microservice.brain.tenant.dto.TenantRequest;
import com.levosoft.microservice.brain.tenant.dto.TenantResponse;
import com.levosoft.microservice.brain.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Administration", description = "Endpoints for managing system tenants and environments")
@CrossOrigin(origins = "http://localhost:5173")
public class TenantAdminController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Provision a new tenant environment", description = "Creates structural metadata registries for an isolated client.")
    public TenantResponse createTenant(@Valid @RequestBody TenantRequest tenantRequest) {
        log.info("API Gateway triggered tenant creation route for username: {}", tenantRequest.username());
        return tenantService.createTenant(tenantRequest);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get tenant details by ID", description = "Retrieves registration and status metadata for a specific tenant.")
    public TenantResponse getTenantById(@PathVariable Long id) {
        log.info("Fetching tenant details for ID: {}", id);
        return tenantService.getTenantById(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List all tenants", description = "Retrieves a comprehensive registry list of all system tenants.")
    public List<TenantResponse> getAllTenants() {
        log.info("Fetching registry list for all tenants");
        return tenantService.listTenants();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update tenant details", description = "Modifies active configurations or structural metadata for an existing tenant.")
    public TenantResponse updateTenant(@PathVariable Long id, @Valid @RequestBody TenantRequest tenantRequest) {
        log.info("Updating tenant configurations for ID: {}", id);
        return tenantService.updateTenant(id, tenantRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decommission a tenant", description = "Removes structural metadata registries for a client.")
    public void deleteTenant(@PathVariable Long id) {
        log.info("Decommissioning tenant environment for ID: {}", id);
        tenantService.deleteTenant(id);
    }
}
