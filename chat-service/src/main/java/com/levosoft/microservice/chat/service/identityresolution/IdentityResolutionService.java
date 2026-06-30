package com.levosoft.microservice.chat.service.identityresolution;

import java.util.Optional;

import com.levosoft.microservice.chat.service.TenantIdentityResolver;

public interface IdentityResolutionService {

    Optional<TenantIdentityResolver.ResolvedIdentity> resolve(String tenantIdentity);
}

