package com.levosoft.microservice.gateway.config;

import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {

    private static final List<String> PUBLIC_BROWSER_PATHS = List.of(
            "/login/**",
            "/oauth2/**",
            "/fallback/**"
    );

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GatewayMvcProperties gatewayProperties) throws Exception {
        RequestMatcher htmlRequestMatcher = new MediaTypeRequestMatcher(org.springframework.http.MediaType.TEXT_HTML);
        AuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak");

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> {
                    // 1. Setup standard public rules
                    authorize
                            .requestMatchers("/actuator/**").permitAll()
                            .requestMatchers(PUBLIC_BROWSER_PATHS.toArray(String[]::new)).permitAll();

                    // 2. Dynamically load protected paths from application.yml metadata
                    if (gatewayProperties.getRoutes() != null) {
                        gatewayProperties.getRoutes().forEach(route -> {
                            Map<String, Object> metadata = route.getMetadata();
                            if (metadata != null && metadata.containsKey("required-role")) {
                                String requiredRole = (String) metadata.get("required-role");

                                // Extract and clean comma-separated Path values
                                List<String> paths = route.getPredicates().stream()
                                        .filter(p -> "Path".equalsIgnoreCase(p.getName()))
                                        .flatMap(p -> p.getArgs().values().stream())
                                        // Split by comma in case multiple predicates are declared inline
                                        .flatMap(pathStr -> java.util.Arrays.stream(pathStr.split(",")))
                                        .map(String::trim)
                                        .collect(Collectors.toList());

                                if (!paths.isEmpty()) {
                                    authorize.requestMatchers(paths.toArray(String[]::new)).hasRole(requiredRole);
                                }
                            }
                        });
                    }

                    // 3. Fallback catch-all
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(loginEntryPoint, htmlRequestMatcher))
                // Fixed: Explicitly mapped the browser login authorities to read Keycloak roles
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> authorities.stream()
                .flatMap(authority -> {
                    if (authority instanceof OidcUserAuthority oidcAuthority) {
                        Map<String, Object> realmAccess = oidcAuthority.getIdToken().getClaim("realm_access");
                        if (realmAccess != null && realmAccess.containsKey("roles")) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) realmAccess.get("roles");
                            return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role));
                        }
                    }
                    return Stream.of(authority);
                })
                .collect(Collectors.toList());
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new Converter<Jwt, Collection<GrantedAuthority>>() {
            @Override
            public Collection<GrantedAuthority> convert(Jwt jwt) {
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                if (realmAccess == null || !realmAccess.containsKey("roles")) {
                    return Collections.emptyList();
                }

                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                return roles.stream()
                        .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                        .collect(Collectors.toList());
            }
        });
        return converter;
    }
}
