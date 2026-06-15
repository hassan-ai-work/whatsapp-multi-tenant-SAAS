package com.levosoft.microservice.gateway.filter;

import org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

@Configuration
public class GatewayUsernameHeaderConfig implements FilterSupplier {

    public static HandlerFilterFunction<ServerResponse, ServerResponse> injectUsername() {
        return (request, next) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated JWT not found");
            }

            String username = jwt.getClaimAsString("preferred_username");
            if (username == null || username.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing preferred_username claim");
            }
            username = username.trim();

            ServerRequest mutatedRequest = ServerRequest.from(request)
                    .header("X-Authenticated-User", username)
                    .build();

            return next.handle(mutatedRequest);
        };
    }

    @Override
    public Collection<Method> get() {
        try {
            return List.of(GatewayUsernameHeaderConfig.class.getMethod("injectUsername"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
