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

import java.util.List;

@RestController
@RequestMapping("/v1/tenant/businesses/channels/*")
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
            @RequestHeader("X-Authenticated-User") String username,
            @Valid @RequestBody ChannelRegistrationRequest request
    ) {
        log.info("API triggered channel registration route for tenant user: {} and business ID: {}", username, request.businessId());
        return tenantRegisteredChannelService.registerChannel(username, request);
    }

    @GetMapping("/{businessId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List business channel registrations", description = "Retrieves all channel registration history for a tenant business.")
    public List<ChannelRegistrationResponse> listBusinessChannels(
            @RequestHeader("X-Authenticated-User") String username,
            @PathVariable Long businessId
    ) {
        log.info("Fetching channel registration list for authenticated user: {} and business ID: {}", username, businessId);
        return tenantRegisteredChannelService.listBusinessChannels(username, businessId);
    }

    @PatchMapping("/link")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Link a channel", description = "Creates an active channel registration for the specified channel code when no active registration currently exists.")
    public ChannelRegistrationResponse linkChannel(
            @RequestHeader("X-Authenticated-User") String username,
            @Valid @RequestBody ChannelRegistrationRequest request
    ) {
        log.info("Linking channel '{}' for authenticated user: {} and business ID: {}", request.channelCode(), username, request.businessId());
        return tenantRegisteredChannelService.linkChannel(username, request);
    }

    @PatchMapping("/unlink")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Unlink a channel", description = "Marks the active channel registration as inactive while preserving row history.")
    public ChannelRegistrationResponse unlinkChannel(
            @RequestHeader("X-Authenticated-User") String username,
            @Valid @RequestBody ChannelRegistrationRequest request
    ) {
        log.info("Unlinking channel '{}' for authenticated user: {} and business ID: {}", request.channelCode(), username, request.businessId());
        return tenantRegisteredChannelService.unlinkChannel(username, request);
    }

    @PostMapping("/replace")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Replace a channel registration", description = "Preserves the current active row as replaced and creates a new active row for the same channel code.")
    public ChannelRegistrationResponse replaceChannel(
            @RequestHeader("X-Authenticated-User") String username,
            @Valid @RequestBody ChannelRegistrationRequest request
    ) {
        log.info("Replacing channel '{}' for authenticated user: {} and business ID: {}", request.channelCode(), username, request.businessId());
        return tenantRegisteredChannelService.replaceChannel(username, request);
    }
}
