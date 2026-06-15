package com.levosoft.microservice.gateway.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class GatewayUsernameHeaderConfigTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenValidJwtWithPreferredUsername_whenInjectUsername_thenHeaderIsInjected() throws Exception {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-1",
                        "preferred_username", "casual.user"
                )
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        AtomicReference<String> propagatedHeader = new AtomicReference<>();

        ServerRequest request = ServerRequest.create(
                new MockHttpServletRequest("GET", "/v1/ingest/docs"),
                java.util.List.of()
        );

        var response = GatewayUsernameHeaderConfig.injectUsername().filter(request, serverRequest -> {
            propagatedHeader.set(serverRequest.headers().firstHeader("X-Authenticated-User"));
            return ServerResponse.ok().build();
        });

        assertThat(response).isNotNull();
        assertThat(propagatedHeader.get()).isEqualTo("casual.user");
    }

    @Test
    void givenJwtWithoutPreferredUsername_whenInjectUsername_thenUnauthorizedIsThrown() {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        ServerRequest request = ServerRequest.create(
                new MockHttpServletRequest("GET", "/v1/ingest/docs"),
                java.util.List.of()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> GatewayUsernameHeaderConfig.injectUsername().filter(request, req -> ServerResponse.ok().build())
        );

        assertThat(exception.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Missing preferred_username claim");
    }

    @Test
    void givenJwtWithBlankPreferredUsername_whenInjectUsername_thenUnauthorizedIsThrown() {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-1",
                        "preferred_username", "   "
                )
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        ServerRequest request = ServerRequest.create(
                new MockHttpServletRequest("GET", "/v1/ingest/docs"),
                java.util.List.of()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> GatewayUsernameHeaderConfig.injectUsername().filter(request, req -> ServerResponse.ok().build())
        );

        assertThat(exception.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Missing preferred_username claim");
    }

    @Test
    void givenMissingJwtPrincipal_whenInjectUsername_thenUnauthorizedIsThrown() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("plain-user", null));

        ServerRequest request = ServerRequest.create(
                new MockHttpServletRequest("GET", "/v1/ingest/docs"),
                java.util.List.of()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> GatewayUsernameHeaderConfig.injectUsername().filter(request, req -> ServerResponse.ok().build())
        );

        assertThat(exception.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Authenticated JWT not found");
    }
}
