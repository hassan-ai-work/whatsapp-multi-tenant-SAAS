package com.levosoft.microservice.brain.tenant.service;

import com.levosoft.microservice.brain.tenant.dto.TenantRequest;
import com.levosoft.microservice.brain.tenant.dto.TenantResponse;
import com.levosoft.microservice.brain.tenant.model.Tenant;
import com.levosoft.microservice.brain.tenant.model.TenantStatus;
import com.levosoft.microservice.brain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final KeycloakIdentityService keycloakIdentityService; // Injected clean boundary service

    @Transactional
    public TenantResponse createTenant(TenantRequest tenantRequest) {
        log.info("Start - validating and provisioning tenant for name: {}", tenantRequest.name());

        if (tenantRepository.existsByName(tenantRequest.name())) {
            log.error("Conflict detected: Tenant name '{}' is already registered", tenantRequest.name());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant name already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(tenantRequest.name());
        tenant.setStatus(TenantStatus.ACTIVE);

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("End - Tenant successfully saved with database sequence key: {}", savedTenant.getId());

        // Trigger Keycloak orchestration via clean sub-service encapsulation
        keycloakIdentityService.provisionKeycloakUser(savedTenant);

        return mapToTenantResponse(savedTenant);
    }

    public TenantResponse getTenantById(Long id) {
        log.info("Searching for tenant with sequence key: {}", id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + id));
        return mapToTenantResponse(tenant);
    }

    public List<TenantResponse> listTenants() {
        log.info("Fetching all registered application tenant boundaries");
        return tenantRepository.findAll()
                .stream()
                .map(this::mapToTenantResponse)
                .toList();
    }

    @Transactional
    public TenantResponse updateTenant(Long id, TenantRequest tenantRequest) {
        log.info("Start - updating tenant configuration for ID: {}", id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + id));

        if (!tenant.getName().equalsIgnoreCase(tenantRequest.name()) && tenantRepository.existsByName(tenantRequest.name())) {
            log.error("Conflict detected: Tenant name '{}' is already taken by another registry", tenantRequest.name());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant name already exists");
        }

        tenant.setName(tenantRequest.name());

        Tenant updatedTenant = tenantRepository.save(tenant);
        log.info("End - Tenant configurations updated successfully for ID: {}", updatedTenant.getId());

        return mapToTenantResponse(updatedTenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        log.info("Attempting hard-delete database purge for tenant ID: {}", id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + id));

        // 1. Drop data inside Keycloak using identity provider boundary module
        keycloakIdentityService.deprovisionKeycloakUser(tenant.getName());

        // 2. Drop database row entity records
        tenantRepository.deleteById(id);
        log.info("Successfully dropped database metadata row for tenant ID: {}", id);
    }

    private TenantResponse mapToTenantResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
