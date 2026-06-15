package com.levosoft.microservice.gateway.config;

import com.levosoft.microservice.gateway.filter.GatewayUsernameHeaderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void givenApplicationYaml_whenInspectingIngestDocsRoute_thenTokenRelayAndInjectUsernameAreConfigured() throws Exception {
        ClassPathResource resource = new ClassPathResource("application.yml");
        String yaml = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

        assertThat(yaml).contains("- id: document_ingest_service");
        assertThat(yaml).contains("uri: http://localhost:8080");
        assertThat(yaml).contains("- Path=/v1/ingest/docs,/v1/ingest/docs/**");
        assertThat(yaml).contains("- TokenRelay=");
        assertThat(yaml).contains("- InjectUsername=");
    }

    @Test
    void givenApplicationYaml_whenInspectingTenantRouteMetadata_thenRequiredRoleRemainsUnchanged() throws Exception {
        ClassPathResource resource = new ClassPathResource("application.yml");
        String yaml = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

        assertThat(yaml).contains("- id: tenant_administration_service");
        assertThat(yaml).contains("required-role: SUPER_ADMIN");
    }

    @Test
    void givenGatewayUsernameHeaderConfig_whenLoaded_thenInjectUsernameMethodIsExposed() {
        var methods = new GatewayUsernameHeaderConfig().get();

        assertThat(methods)
                .extracting(Method::getName)
                .contains("injectUsername");
    }

    @Test
    void givenJwtWithRealmRoles_whenMappingAuthorities_thenRoleAuthoritiesAreProduced() throws Exception {
        Method converterMethod = SecurityConfig.class.getDeclaredMethod("jwtAuthenticationConverter");
        converterMethod.setAccessible(true);

        Object converterObject = converterMethod.invoke(securityConfig);
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter =
                (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter) converterObject;

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-1",
                        "realm_access", Map.of("roles", List.of("SUPER_ADMIN", "USER"))
                )
        );

        var authentication = converter.convert(jwt);
        assertThat(authentication).isNotNull();

        Collection<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        assertThat(authorities)
                .containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_USER");
    }

    @Test
    void givenJwtWithoutRealmRoles_whenMappingAuthorities_thenNoRoleAuthoritiesAreProduced() throws Exception {
        Method converterMethod = SecurityConfig.class.getDeclaredMethod("jwtAuthenticationConverter");
        converterMethod.setAccessible(true);

        Object converterObject = converterMethod.invoke(securityConfig);
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter =
                (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter) converterObject;

        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );

        var authentication = converter.convert(jwt);
        assertThat(authentication).isNotNull();

        Collection<String> roleAuthorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        assertThat(roleAuthorities).isEmpty();
    }
}
