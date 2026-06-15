package com.levosoft.microservice.brain.channel.controller;

import com.levosoft.microservice.brain.channel.dto.ChannelRegistrationRequest;
import com.levosoft.microservice.brain.channel.dto.ChannelRegistrationResponse;
import com.levosoft.microservice.brain.channel.service.TenantRegisteredChannelService;
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
@RequestMapping("/v1/tenants/{tenantId}/businesses/{businessId}/channels")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Channel Registration", description = "Endpoints for managing tenant business channel registrations")
@CrossOrigin(origins = "http://localhost:5173")
public class TenantRegisteredChannelController {

    private final TenantRegisteredChannelService tenantRegisteredChannelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a business channel", description = "Creates a new active registration for a business channel when no active row already exists.")
    public ChannelRegistrationResponse registerChannel(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @RequestHeader("X-Authenticated-User") String authenticatedUser,
            @Valid @RequestBody ChannelRegistrationRequest request
    ) {
        log.info("API triggered channel registration route for tenant ID: {} and business ID: {}", tenantId, businessId);
        return tenantRegisteredChannelService.registerChannel(authenticatedUser, tenantId, businessId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List business channel registrations", description = "Retrieves all channel registration history for a tenant business.")
    public List<ChannelRegistrationResponse> listBusinessChannels(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @RequestHeader("X-Authenticated-User") String authenticatedUser
    ) {
        log.info("Fetching channel registration list for tenant ID: {} and business ID: {}", tenantId, businessId);
        return tenantRegisteredChannelService.listBusinessChannels(authenticatedUser, tenantId, businessId);
    }

    @PatchMapping("/{channelCode}/link")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Link a channel", description = "Creates an active channel registration for the specified channel code when no active registration currently exists.")
    public ChannelRegistrationResponse linkChannel(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @PathVariable String channelCode,
            @RequestHeader("X-Authenticated-User") String authenticatedUser
    ) {
        log.info("Linking channel '{}' for tenant ID: {} and business ID: {}", channelCode, tenantId, businessId);
        return tenantRegisteredChannelService.linkChannel(authenticatedUser, tenantId, businessId, channelCode);
    }

    @PatchMapping("/{channelCode}/unlink")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unlink a channel", description = "Marks the active channel registration as inactive while preserving row history.")
    public ChannelRegistrationResponse unlinkChannel(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @PathVariable String channelCode,
            @RequestHeader("X-Authenticated-User") String authenticatedUser
    ) {
        log.info("Unlinking channel '{}' for tenant ID: {} and business ID: {}", channelCode, tenantId, businessId);
        return tenantRegisteredChannelService.unlinkChannel(authenticatedUser, tenantId, businessId, channelCode);
    }

    @PostMapping("/{channelCode}/replace")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Replace a channel registration", description = "Preserves the current active row as replaced and creates a new active row for the same channel code.")
    public ChannelRegistrationResponse replaceChannel(
            @PathVariable Long tenantId,
            @PathVariable Long businessId,
            @PathVariable String channelCode,
            @RequestHeader("X-Authenticated-User") String authenticatedUser,
            @Valid @RequestBody ChannelRegistrationRequest request
    ) {
        log.info("Replacing channel '{}' for tenant ID: {} and business ID: {}", channelCode, tenantId, businessId);

        if (!channelCode.equalsIgnoreCase(request.channelCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "channelCode in path must match channelCode in request body");
        }

        return tenantRegisteredChannelService.replaceChannel(authenticatedUser, tenantId, businessId, channelCode, request);
    }
}
