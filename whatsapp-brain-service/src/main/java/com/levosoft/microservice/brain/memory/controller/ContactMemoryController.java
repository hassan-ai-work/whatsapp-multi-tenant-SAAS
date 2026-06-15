package com.levosoft.microservice.brain.memory.controller;

import com.levosoft.microservice.brain.memory.dto.ContactMemoryRequest;
import com.levosoft.microservice.brain.memory.dto.ContactMemoryResponse;
import com.levosoft.microservice.brain.memory.service.ContactMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/businesses/{businessId}/memories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contact Memory", description = "Endpoints for managing tenant business contact memory records")
@CrossOrigin(origins = "http://localhost:5173")
public class ContactMemoryController {

    private final ContactMemoryService contactMemoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a contact memory", description = "Creates a tenant business scoped contact memory record for future AI reference.")
    public ContactMemoryResponse createMemory(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @RequestHeader("X-Authenticated-User") String authenticatedUser,
            @Valid @RequestBody ContactMemoryRequest request
    ) {
        log.info("API triggered contact memory creation for tenant ID: {} and business ID: {}", tenantId, businessId);
        return contactMemoryService.createMemory(authenticatedUser, tenantId, businessId, request);
    }

    @GetMapping("/{memoryId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get contact memory by ID", description = "Retrieves a specific contact memory owned by a tenant business.")
    public ContactMemoryResponse getMemoryById(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @PathVariable Long memoryId,
            @RequestHeader("X-Authenticated-User") String authenticatedUser
    ) {
        log.info("Fetching contact memory details for memory ID: {}, tenant ID: {}, business ID: {}", memoryId, tenantId, businessId);
        return contactMemoryService.getMemoryById(authenticatedUser, tenantId, businessId, memoryId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List contact memories", description = "Retrieves all contact memories for a tenant business.")
    public List<ContactMemoryResponse> listMemories(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @RequestHeader("X-Authenticated-User") String authenticatedUser
    ) {
        log.info("Fetching contact memory list for tenant ID: {} and business ID: {}", tenantId, businessId);
        return contactMemoryService.listMemories(authenticatedUser, tenantId, businessId);
    }
}
