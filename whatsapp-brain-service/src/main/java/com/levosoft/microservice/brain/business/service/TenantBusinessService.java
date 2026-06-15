package com.levosoft.microservice.brain.business.service;

import com.levosoft.microservice.brain.business.dto.TenantBusinessRequest;
import com.levosoft.microservice.brain.business.dto.TenantBusinessResponse;
import com.levosoft.microservice.brain.business.model.TenantBusiness;
import com.levosoft.microservice.brain.business.repository.TenantBusinessRepository;
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
public class TenantBusinessService {

    private final TenantBusinessRepository tenantBusinessRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public TenantBusinessResponse createBusiness(String username, TenantBusinessRequest businessRequest) {
        Long resolvedTenantId = resolveTenantId(username);

        log.info("Start - validating and creating business for tenant ID: {}", resolvedTenantId);

        if (tenantBusinessRepository.existsByRegisteredNumber(businessRequest.registeredNumber())) {
            log.error("Conflict detected: Business registered number '{}' is already registered", businessRequest.registeredNumber());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Business registered number already exists");
        }

        TenantBusiness tenantBusiness = new TenantBusiness();
        tenantBusiness.setTenantId(resolvedTenantId);
        tenantBusiness.setBusinessName(businessRequest.businessName());
        tenantBusiness.setDescription(businessRequest.description());
        tenantBusiness.setRegisteredNumber(businessRequest.registeredNumber());

        TenantBusiness savedBusiness = tenantBusinessRepository.save(tenantBusiness);
        log.info("End - Business successfully saved with database sequence key: {}", savedBusiness.getId());

        return mapToResponse(savedBusiness);
    }

    public TenantBusinessResponse getBusinessById(String username, Long businessId) {
        Long resolvedTenantId = resolveTenantId(username);

        log.info("Searching for business ID: {} under tenant ID: {}", businessId, resolvedTenantId);
        TenantBusiness tenantBusiness = tenantBusinessRepository.findByIdAndTenantId(businessId, resolvedTenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found with ID: " + businessId + " for tenant ID: " + resolvedTenantId));
        return mapToResponse(tenantBusiness);
    }

    public List<TenantBusinessResponse> listBusinessesByTenantId(String username) {
        Long resolvedTenantId = resolveTenantId(username);

        log.info("Fetching all businesses for tenant ID: {}", resolvedTenantId);

        return tenantBusinessRepository.findAllByTenantId(resolvedTenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TenantBusinessResponse updateBusiness(String username, Long businessId, TenantBusinessRequest businessRequest) {
        Long resolvedTenantId = resolveTenantId(username);

        log.info("Start - updating business ID: {} for tenant ID: {}", businessId, resolvedTenantId);

        TenantBusiness tenantBusiness = tenantBusinessRepository.findByIdAndTenantId(businessId, resolvedTenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found with ID: " + businessId + " for tenant ID: " + resolvedTenantId));

        if (!tenantBusiness.getRegisteredNumber().equalsIgnoreCase(businessRequest.registeredNumber())
                && tenantBusinessRepository.existsByRegisteredNumber(businessRequest.registeredNumber())) {
            log.error("Conflict detected: Business registered number '{}' is already taken by another registry", businessRequest.registeredNumber());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Business registered number already exists");
        }

        tenantBusiness.setBusinessName(businessRequest.businessName());
        tenantBusiness.setDescription(businessRequest.description());
        tenantBusiness.setRegisteredNumber(businessRequest.registeredNumber());

        TenantBusiness updatedBusiness = tenantBusinessRepository.save(tenantBusiness);
        log.info("End - Business updated successfully for ID: {}", updatedBusiness.getId());

        return mapToResponse(updatedBusiness);
    }

    @Transactional
    public void deleteBusiness(String username, Long businessId) {
        Long resolvedTenantId = resolveTenantId(username);

        log.info("Attempting hard-delete database purge for business ID: {} under tenant ID: {}", businessId, resolvedTenantId);

        TenantBusiness tenantBusiness = tenantBusinessRepository.findByIdAndTenantId(businessId, resolvedTenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found with ID: " + businessId + " for tenant ID: " + resolvedTenantId));

        tenantBusinessRepository.deleteById(tenantBusiness.getId());
        log.info("Successfully dropped database metadata row for business ID: {}", businessId);
    }

    private Long resolveTenantId(String authenticatedUsername) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or blank X-Authenticated-User header");
        }

        String normalizedUsername = authenticatedUsername.trim().toLowerCase();

        return tenantRepository.findByUsername(normalizedUsername)
                .map(tenant -> tenant.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found for authenticated user: " + normalizedUsername));
    }

    private TenantBusinessResponse mapToResponse(TenantBusiness tenantBusiness) {
        return new TenantBusinessResponse(
                tenantBusiness.getId(),
                tenantBusiness.getTenantId(),
                tenantBusiness.getBusinessName(),
                tenantBusiness.getDescription(),
                tenantBusiness.getRegisteredNumber(),
                tenantBusiness.getCreatedAt(),
                tenantBusiness.getUpdatedAt()
        );
    }
}
