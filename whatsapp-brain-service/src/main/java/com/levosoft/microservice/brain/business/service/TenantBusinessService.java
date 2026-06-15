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
    public TenantBusinessResponse createBusiness(TenantBusinessRequest businessRequest) {
        log.info("Start - validating and creating business for tenant ID: {}", businessRequest.tenantId());

        if (!tenantRepository.existsById(businessRequest.tenantId())) {
            log.error("Tenant not found for ID: {}", businessRequest.tenantId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + businessRequest.tenantId());
        }

        if (tenantBusinessRepository.existsByRegisteredNumber(businessRequest.registeredNumber())) {
            log.error("Conflict detected: Business registered number '{}' is already registered", businessRequest.registeredNumber());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Business registered number already exists");
        }

        TenantBusiness tenantBusiness = new TenantBusiness();
        tenantBusiness.setTenantId(businessRequest.tenantId());
        tenantBusiness.setBusinessName(businessRequest.businessName());
        tenantBusiness.setDescription(businessRequest.description());
        tenantBusiness.setRegisteredNumber(businessRequest.registeredNumber());

        TenantBusiness savedBusiness = tenantBusinessRepository.save(tenantBusiness);
        log.info("End - Business successfully saved with database sequence key: {}", savedBusiness.getId());

        return mapToResponse(savedBusiness);
    }

    public TenantBusinessResponse getBusinessById(Long tenantId, Long businessId) {
        log.info("Searching for business ID: {} under tenant ID: {}", businessId, tenantId);
        TenantBusiness tenantBusiness = tenantBusinessRepository.findByIdAndTenantId(businessId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found with ID: " + businessId + " for tenant ID: " + tenantId));
        return mapToResponse(tenantBusiness);
    }

    public List<TenantBusinessResponse> listBusinessesByTenantId(Long tenantId) {
        log.info("Fetching all businesses for tenant ID: {}", tenantId);

        if (!tenantRepository.existsById(tenantId)) {
            log.error("Tenant not found for ID: {}", tenantId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + tenantId);
        }

        return tenantBusinessRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TenantBusinessResponse updateBusiness(Long tenantId, Long businessId, TenantBusinessRequest businessRequest) {
        log.info("Start - updating business ID: {} for tenant ID: {}", businessId, tenantId);

        if (!tenantId.equals(businessRequest.tenantId())) {
            log.error("Ownership mismatch detected for tenant ID path: {} and payload tenant ID: {}", tenantId, businessRequest.tenantId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId in path must match tenantId in request body");
        }

        TenantBusiness tenantBusiness = tenantBusinessRepository.findByIdAndTenantId(businessId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found with ID: " + businessId + " for tenant ID: " + tenantId));

        if (!tenantBusiness.getRegisteredNumber().equalsIgnoreCase(businessRequest.registeredNumber())
                && tenantBusinessRepository.existsByRegisteredNumber(businessRequest.registeredNumber())) {
            log.error("Conflict detected: Business registered number '{}' is already taken by another registry", businessRequest.registeredNumber());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Business registered number already exists");
        }

        tenantBusiness.setTenantId(businessRequest.tenantId());
        tenantBusiness.setBusinessName(businessRequest.businessName());
        tenantBusiness.setDescription(businessRequest.description());
        tenantBusiness.setRegisteredNumber(businessRequest.registeredNumber());

        TenantBusiness updatedBusiness = tenantBusinessRepository.save(tenantBusiness);
        log.info("End - Business updated successfully for ID: {}", updatedBusiness.getId());

        return mapToResponse(updatedBusiness);
    }

    @Transactional
    public void deleteBusiness(Long tenantId, Long businessId) {
        log.info("Attempting hard-delete database purge for business ID: {} under tenant ID: {}", businessId, tenantId);

        TenantBusiness tenantBusiness = tenantBusinessRepository.findByIdAndTenantId(businessId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found with ID: " + businessId + " for tenant ID: " + tenantId));

        tenantBusinessRepository.deleteById(tenantBusiness.getId());
        log.info("Successfully dropped database metadata row for business ID: {}", businessId);
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
