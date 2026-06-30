package com.levosoft.microservice.chat.service;

import java.util.Optional;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.levosoft.microservice.chat.service.identityresolution.IdentityResolutionService;

@Service
public class TenantIdentityResolver implements IdentityResolutionService {

    public record ResolvedIdentity(long tenantId, long businessId, String businessName) {}

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TenantIdentityResolver(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ResolvedIdentity> resolve(String tenantIdentity) {
        String sql = """
                SELECT id AS business_id, tenant_id, business_name
                FROM tenant_business
                WHERE registered_number = :tenant_identity
                """;
        return jdbcTemplate.query(sql,
                java.util.Map.of("tenant_identity", tenantIdentity),
                rs -> rs.next()
                        ? Optional.of(new ResolvedIdentity(
                        rs.getLong("tenant_id"),
                        rs.getLong("business_id"),
                        rs.getString("business_name")))
                        : Optional.empty());
    }
}

