package com.levosoft.microservice.brain.business.controller;

import com.levosoft.microservice.brain.business.dto.TenantBusinessRequest;
import com.levosoft.microservice.brain.business.dto.TenantBusinessResponse;
import com.levosoft.microservice.brain.business.service.TenantBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenant/businesses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Business Administration", description = "Endpoints for managing tenant-owned business registrations")
public class TenantBusinessController {

    private final TenantBusinessService tenantBusinessService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new business for a tenant", description = "Creates a business record owned by the authenticated tenant.")
    public TenantBusinessResponse createBusiness(
            @RequestHeader("X-Authenticated-User") String username,
            @Valid @RequestBody TenantBusinessRequest businessRequest
    ) {
        log.info("API triggered business creation route for username: {}", username);
        return tenantBusinessService.createBusiness(username, businessRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List tenant businesses", description = "Retrieves all business registrations owned by the specified tenant.")
    public List<TenantBusinessResponse> getAllBusinesses(
            @RequestHeader("X-Authenticated-User") String username
    ) {
        log.info("Fetching business registry list for username: {}", username);
        return tenantBusinessService.listBusinessesByTenantId(username);
    }

    @GetMapping("/{businessId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get business details by ID", description = "Retrieves business registration metadata owned by the authenticated tenant.")
    public TenantBusinessResponse getBusinessById(
            @PathVariable Long businessId,
            @RequestHeader("X-Authenticated-User") String username
    ) {
        log.info("Fetching business details for business ID: {} and username: {}", businessId, username);
        return tenantBusinessService.getBusinessById(username, businessId);
    }

    @PutMapping("/{businessId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update business details", description = "Modifies an existing tenant-owned business registration.")
    public TenantBusinessResponse updateBusiness(
            @PathVariable Long businessId,
            @RequestHeader("X-Authenticated-User") String username,
            @Valid @RequestBody TenantBusinessRequest businessRequest
    ) {
        log.info("Updating business configurations for business ID: {} and username: {}", businessId, username);
        return tenantBusinessService.updateBusiness(username, businessId, businessRequest);
    }

    @DeleteMapping("/{businessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a tenant business", description = "Removes a tenant-owned business registration.")
    public void deleteBusiness(
            @PathVariable Long businessId,
            @RequestHeader("X-Authenticated-User") String username
    ) {
        log.info("Decommissioning business registration for business ID: {} and username: {}", businessId, username);
        tenantBusinessService.deleteBusiness(username, businessId);
    }
}
