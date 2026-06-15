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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/businesses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Business Administration", description = "Endpoints for managing tenant-owned business registrations")
@CrossOrigin(origins = "http://localhost:5173")
public class TenantBusinessController {

    private final TenantBusinessService tenantBusinessService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new business for a tenant", description = "Creates a business record owned by a specific tenant.")
    public TenantBusinessResponse createBusiness(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantBusinessRequest businessRequest
    ) {
        log.info("API triggered business creation route for tenant ID: {}", tenantId);

        if (!tenantId.equals(businessRequest.tenantId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "tenantId in path must match tenantId in request body"
            );
        }

        return tenantBusinessService.createBusiness(businessRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List tenant businesses", description = "Retrieves all business registrations owned by the specified tenant.")
    public List<TenantBusinessResponse> getAllBusinesses(@PathVariable Long tenantId) {
        log.info("Fetching business registry list for tenant ID: {}", tenantId);
        return tenantBusinessService.listBusinessesByTenantId(tenantId);
    }

    @GetMapping("/{businessId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get business details by ID", description = "Retrieves business registration metadata owned by the specified tenant.")
    public TenantBusinessResponse getBusinessById(
            @PathVariable Long tenantId,
            @PathVariable Long businessId
    ) {
        log.info("Fetching business details for business ID: {} and tenant ID: {}", businessId, tenantId);
        return tenantBusinessService.getBusinessById(tenantId, businessId);
    }

    @PutMapping("/{businessId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update business details", description = "Modifies an existing tenant-owned business registration.")
    public TenantBusinessResponse updateBusiness(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @Valid @RequestBody TenantBusinessRequest businessRequest
    ) {
        log.info("Updating business configurations for business ID: {} and tenant ID: {}", businessId, tenantId);

        if (!tenantId.equals(businessRequest.tenantId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "tenantId in path must match tenantId in request body"
            );
        }

        return tenantBusinessService.updateBusiness(tenantId, businessId, businessRequest);
    }

    @DeleteMapping("/{businessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a tenant business", description = "Removes a tenant-owned business registration.")
    public void deleteBusiness(
            @PathVariable Long tenantId,
            @PathVariable Long businessId
    ) {
        log.info("Decommissioning business registration for business ID: {} and tenant ID: {}", businessId, tenantId);
        tenantBusinessService.deleteBusiness(tenantId, businessId);
    }
}
