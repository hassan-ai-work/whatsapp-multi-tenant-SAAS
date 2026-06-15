package com.levosoft.microservice.brain.memory.service;

import com.levosoft.microservice.brain.business.repository.TenantBusinessRepository;
import com.levosoft.microservice.brain.memory.dto.ContactMemoryRequest;
import com.levosoft.microservice.brain.memory.dto.ContactMemoryResponse;
import com.levosoft.microservice.brain.memory.model.ContactMemory;
import com.levosoft.microservice.brain.memory.repository.ContactMemoryRepository;
import com.levosoft.microservice.brain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactMemoryService {

    private final ContactMemoryRepository contactMemoryRepository;
    private final TenantRepository tenantRepository;
    private final TenantBusinessRepository tenantBusinessRepository;

    @Transactional
    public ContactMemoryResponse createMemory(ContactMemoryRequest request) {
        log.info("Start - creating contact memory for tenant ID: {}, business ID: {}, contactIdentifier: {}",
                request.tenantId(), request.businessId(), request.contactIdentifier());

        validateTenantExists(request.tenantId());
        validateBusinessOwnership(request.tenantId(), request.businessId());

        return contactMemoryRepository.findByTenantIdAndBusinessIdAndIdempotencyHash(
                        request.tenantId(),
                        request.businessId(),
                        request.idempotencyHash()
                )
                .map(existing -> {
                    log.info("Idempotent retry detected for hash: {}. Returning existing memory ID: {}", request.idempotencyHash(), existing.getId());
                    return mapToResponse(existing);
                })
                .orElseGet(() -> {
                    ContactMemory memory = new ContactMemory();
                    memory.setTenantId(request.tenantId());
                    memory.setBusinessId(request.businessId());
                    memory.setContactIdentifier(request.contactIdentifier().trim());
                    memory.setContactChatText(request.contactChatText().trim());
                    memory.setEmbedding(toPgVector(request.embedding()));
                    memory.setIdempotencyHash(request.idempotencyHash().trim());

                    ContactMemory savedMemory = contactMemoryRepository.save(memory);
                    log.info("End - contact memory saved successfully with ID: {}", savedMemory.getId());
                    return mapToResponse(savedMemory);
                });
    }

    public ContactMemoryResponse getMemoryById(Long tenantId, Long businessId, Long memoryId) {
        log.info("Fetching contact memory ID: {} for tenant ID: {} and business ID: {}", memoryId, tenantId, businessId);

        validateTenantExists(tenantId);
        validateBusinessOwnership(tenantId, businessId);

        ContactMemory memory = contactMemoryRepository.findByIdAndTenantIdAndBusinessId(memoryId, tenantId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact memory not found with ID: " + memoryId));

        return mapToResponse(memory);
    }

    public List<ContactMemoryResponse> listMemories(Long tenantId, Long businessId) {
        log.info("Listing contact memories for tenant ID: {} and business ID: {}", tenantId, businessId);

        validateTenantExists(tenantId);
        validateBusinessOwnership(tenantId, businessId);

        return contactMemoryRepository.findAllByTenantIdAndBusinessIdOrderByCreatedAtDesc(tenantId, businessId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateTenantExists(Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            log.error("Tenant not found for ID: {}", tenantId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + tenantId);
        }
    }

    private void validateBusinessOwnership(Long tenantId, Long businessId) {
        tenantBusinessRepository.findByIdAndTenantId(businessId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Business not found with ID: " + businessId + " for tenant ID: " + tenantId
                ));
    }

    private String toPgVector(List<Double> values) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Double value : values) {
            joiner.add(value.toString());
        }
        return joiner.toString();
    }

    private List<Float> toFloatList(String vector) {
        List<Float> result = new ArrayList<>();
        if (vector == null || vector.isBlank()) {
            return result;
        }
        String trimmed = vector.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return result;
        }
        for (String part : trimmed.split(",")) {
            result.add(Float.parseFloat(part.trim()));
        }
        return result;
    }

    private ContactMemoryResponse mapToResponse(ContactMemory memory) {
        return new ContactMemoryResponse(
                memory.getId(),
                memory.getTenantId(),
                memory.getBusinessId(),
                memory.getContactIdentifier(),
                memory.getContactChatText(),
                toFloatList(memory.getEmbedding()),
                memory.getIdempotencyHash(),
                memory.getCreatedAt(),
                memory.getUpdatedAt()
        );
    }
}
