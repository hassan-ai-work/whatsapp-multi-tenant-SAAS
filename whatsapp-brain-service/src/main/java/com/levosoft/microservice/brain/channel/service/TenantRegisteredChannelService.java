package com.levosoft.microservice.brain.channel.service;

import com.levosoft.microservice.brain.business.model.TenantBusiness;
import com.levosoft.microservice.brain.business.repository.TenantBusinessRepository;
import com.levosoft.microservice.brain.channel.dto.ChannelRegistrationRequest;
import com.levosoft.microservice.brain.channel.dto.ChannelRegistrationResponse;
import com.levosoft.microservice.brain.channel.model.Channel;
import com.levosoft.microservice.brain.channel.model.TenantRegisteredChannel;
import com.levosoft.microservice.brain.channel.repository.ChannelRepository;
import com.levosoft.microservice.brain.channel.repository.TenantRegisteredChannelRepository;
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
public class TenantRegisteredChannelService {

    private static final Boolean LINKED_STATUS_ACTIVE = true;
    private static final Boolean LINKED_STATUS_INACTIVE = false;

    private final TenantRegisteredChannelRepository tenantRegisteredChannelRepository;
    private final ChannelRepository channelRepository;
    private final TenantBusinessRepository tenantBusinessRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public ChannelRegistrationResponse registerChannel(String authenticatedUsername, ChannelRegistrationRequest request) {
        Long resolvedTenantId = resolveTenantId(authenticatedUsername);

        RegistrationValidationContext validationContext = validateRegistrationRequest(resolvedTenantId, request);

        TenantRegisteredChannel registration = new TenantRegisteredChannel();
        registration.setBusinessId(request.businessId());
        registration.setChannelCode(validationContext.channel().getCode());
        registration.setDisplayName(trimToNull(request.displayName()));
        registration.setLinkedStatus(LINKED_STATUS_INACTIVE); //at channel creation its false, it will be set to true when the channel is linked for the first time

        TenantRegisteredChannel savedRegistration = tenantRegisteredChannelRepository.save(registration);
        log.info("End - channel registered successfully with ID: {}", savedRegistration.getId());

        return mapToResponse(savedRegistration, validationContext.channel().getCode());
    }

    public List<ChannelRegistrationResponse> listBusinessChannels(String authenticatedUsername, Long businessId) {
        Long resolvedTenantId = resolveTenantId(authenticatedUsername);

        log.info("Fetching registered channels for tenant ID: {} and business ID: {}", resolvedTenantId, businessId);

        validateBusinessOwnership(resolvedTenantId, businessId);

        return tenantRegisteredChannelRepository.findAllByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ChannelRegistrationResponse linkChannel(String authenticatedUsername, ChannelRegistrationRequest request) {
        Long resolvedTenantId = resolveTenantId(authenticatedUsername);

        log.info("Linking channel '{}' for tenant ID: {} and business ID: {}", request.channelCode(), resolvedTenantId, request.businessId());

        ChannelValidationContext validationContext = validateBusinessAndResolveChannel(resolvedTenantId, request.businessId(), request.channelCode());

        TenantRegisteredChannel activeRegistration = findActiveRegistration(request.businessId(), validationContext.normalizedChannelCode());
        if (activeRegistration != null) {
            log.info(
                    "Channel '{}' is already linked for tenant ID: {} and business ID: {}",
                    validationContext.normalizedChannelCode(),
                    resolvedTenantId,
                    request.businessId()
            );
            return mapToResponse(activeRegistration, validationContext.channel().getCode());
        }

        TenantRegisteredChannel registration = new TenantRegisteredChannel();
        registration.setBusinessId(request.businessId());
        registration.setChannelCode(validationContext.channel().getCode());
        registration.setDisplayName(trimToNull(request.displayName()) != null ? trimToNull(request.displayName()) : validationContext.channel().getCode());
        registration.setLinkedStatus(LINKED_STATUS_ACTIVE);

        TenantRegisteredChannel savedRegistration = tenantRegisteredChannelRepository.save(registration);
        log.info(
                "Channel '{}' linked successfully with registration ID: {}",
                validationContext.normalizedChannelCode(),
                savedRegistration.getId()
        );

        return mapToResponse(savedRegistration, validationContext.channel().getCode());
    }

    @Transactional
    public ChannelRegistrationResponse unlinkChannel(String authenticatedUsername, ChannelRegistrationRequest request) {
        Long resolvedTenantId = resolveTenantId(authenticatedUsername);

        log.info("Unlinking channel '{}' for tenant ID: {} and business ID: {}", request.channelCode(), resolvedTenantId, request.businessId());

        ChannelValidationContext validationContext = validateBusinessAndResolveChannel(resolvedTenantId, request.businessId(), request.channelCode());

        TenantRegisteredChannel activeRegistration = requireActiveRegistration(
                request.businessId(),
                validationContext.normalizedChannelCode()
        );

        activeRegistration.setLinkedStatus(LINKED_STATUS_INACTIVE);

        TenantRegisteredChannel savedRegistration = tenantRegisteredChannelRepository.save(activeRegistration);
        log.info(
                "Channel '{}' unlinked successfully for registration ID: {}",
                validationContext.normalizedChannelCode(),
                savedRegistration.getId()
        );

        return mapToResponse(savedRegistration, validationContext.channel().getCode());
    }

    @Transactional
    public ChannelRegistrationResponse replaceChannel(String authenticatedUsername, ChannelRegistrationRequest request) {
        Long resolvedTenantId = resolveTenantId(authenticatedUsername);

        log.info("Replacing channel '{}' for tenant ID: {} and business ID: {}", request.channelCode(), resolvedTenantId, request.businessId());

        ChannelValidationContext validationContext = validateBusinessAndResolveChannel(resolvedTenantId, request.businessId(), request.channelCode());

        TenantRegisteredChannel activeRegistration = findActiveRegistration(
                request.businessId(),
                validationContext.normalizedChannelCode()
        );
        if (activeRegistration != null) {
            activeRegistration.setLinkedStatus(LINKED_STATUS_INACTIVE);
            tenantRegisteredChannelRepository.save(activeRegistration);
        }

        TenantRegisteredChannel replacement = new TenantRegisteredChannel();
        replacement.setBusinessId(request.businessId());
        replacement.setChannelCode(validationContext.channel().getCode());
        replacement.setDisplayName(trimToNull(request.displayName()));
        replacement.setLinkedStatus(LINKED_STATUS_ACTIVE);

        TenantRegisteredChannel savedReplacement = tenantRegisteredChannelRepository.save(replacement);
        log.info(
                "Channel '{}' replaced successfully with registration ID: {}",
                validationContext.normalizedChannelCode(),
                savedReplacement.getId()
        );

        return mapToResponse(savedReplacement, validationContext.channel().getCode());
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

    private RegistrationValidationContext validateRegistrationRequest(Long tenantId, ChannelRegistrationRequest request) {
        ChannelValidationContext channelValidationContext = validateBusinessAndResolveChannel(
                tenantId,
                request.businessId(),
                request.channelCode()
        );

        validateNoActiveRegistration(
                tenantId,
                request.businessId(),
                channelValidationContext.normalizedChannelCode()
        );

        return new RegistrationValidationContext(
                channelValidationContext.channel(),
                channelValidationContext.normalizedChannelCode()
        );
    }

    private ChannelValidationContext validateBusinessAndResolveChannel(Long tenantId, Long businessId, String channelCode) {
        validateBusinessOwnership(tenantId, businessId);

        String normalizedChannelCode = normalizeChannelCode(channelCode);
        Channel channel = getChannelByCode(normalizedChannelCode);

        return new ChannelValidationContext(channel, normalizedChannelCode);
    }

    private void validateNoActiveRegistration(Long tenantId, Long businessId, String normalizedChannelCode) {
        TenantRegisteredChannel existingActiveRegistration = findActiveRegistration(businessId, normalizedChannelCode);

        if (existingActiveRegistration != null) {
            log.error(
                    "Conflict detected: active channel registration already exists for tenant ID: {}, business ID: {}, channelCode: {}",
                    tenantId,
                    businessId,
                    normalizedChannelCode
            );
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Active channel registration already exists for channel code: " + normalizedChannelCode
            );
        }
    }

    private void validateBusinessOwnership(Long tenantId, Long businessId) {
        TenantBusiness business = tenantBusinessRepository.findByIdAndTenantId(businessId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Business not found with ID: " + businessId + " for tenant ID: " + tenantId
                ));
        log.debug("Validated business ownership for business ID: {} under tenant ID: {}", business.getId(), tenantId);
    }

    private String normalizeChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "channelCode is required");
        }
        return channelCode.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Channel getChannelByCode(String channelCode) {
        return channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found with code: " + channelCode));
    }

    private TenantRegisteredChannel findActiveRegistration(Long businessId, String channelCode) {
        return tenantRegisteredChannelRepository
                .findFirstByBusinessIdAndChannelCodeAndLinkedStatusOrderByCreatedAtDesc(
                        businessId,
                        channelCode,
                        LINKED_STATUS_ACTIVE
                )
                .orElse(null);
    }

    private TenantRegisteredChannel requireActiveRegistration(Long businessId, String channelCode) {
        TenantRegisteredChannel activeRegistration = findActiveRegistration(businessId, channelCode);
        if (activeRegistration == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Active channel registration not found for channel code: " + channelCode
            );
        }
        return activeRegistration;
    }

    private ChannelRegistrationResponse mapToResponse(TenantRegisteredChannel registration) {
        Channel channel = channelRepository.findByCode(registration.getChannelCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel not found with code: " + registration.getChannelCode()));
        return mapToResponse(registration, channel.getCode());
    }

    private ChannelRegistrationResponse mapToResponse(TenantRegisteredChannel registration, String channelName) {
        return new ChannelRegistrationResponse(
                registration.getId(),
                registration.getBusinessId(),
                registration.getChannelCode(),
                channelName,
                registration.getDisplayName(),
                registration.getLinkedStatus(),
                registration.getCreatedAt(),
                registration.getUpdatedAt()
        );
    }

    private record ChannelValidationContext(Channel channel, String normalizedChannelCode) {
    }

    private record RegistrationValidationContext(Channel channel, String normalizedChannelCode) {
    }
}
