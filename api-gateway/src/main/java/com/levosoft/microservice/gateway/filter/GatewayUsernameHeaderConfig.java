package com.levosoft.microservice.gateway.filter;

import org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

@Configuration
public class GatewayUsernameHeaderConfig implements FilterSupplier {

    // 1. Define the actual filter execution logic
    public static HandlerFilterFunction<ServerResponse, ServerResponse> injectUsername() {
        return (request, next) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String username = jwt.getClaimAsString("preferred_username");
                if (username != null) {
                    // Mutate the incoming request to include the header
                    request = ServerRequest.from(request)
                            .header("X-Authenticated-User", username)
                            .build();
                }
            }
            return next.handle(request);
        };
    }

    // 2. Register the filter name so application.yml can see it
    @Override
    public Collection<Method> get() {
        try {
            return List.of(GatewayUsernameHeaderConfig.class.getMethod("injectUsername"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
