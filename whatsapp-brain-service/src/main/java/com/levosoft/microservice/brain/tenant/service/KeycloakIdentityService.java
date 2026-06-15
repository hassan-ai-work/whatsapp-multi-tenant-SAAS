package com.levosoft.microservice.brain.tenant.service;

import com.levosoft.microservice.brain.tenant.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakIdentityService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.username}")
    private String adminUsername;

    @Value("${keycloak.password}")
    private String adminPassword;

    private Keycloak buildKeycloakClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    public void provisionKeycloakUser(Tenant tenant) {
        log.info("Connecting to Keycloak Admin API endpoint target for creation: {}", serverUrl);

        // Optimization 1: Embedded credential directly in the initial payload to eliminate the 2nd network trip
        CredentialRepresentation passwordCredential = new CredentialRepresentation();
        passwordCredential.setType(CredentialRepresentation.PASSWORD);
        passwordCredential.setValue("test");
        passwordCredential.setTemporary(false);

        // STEP 1: Construct the basic user profile metadata container
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(tenant.getUsername().toLowerCase().trim()); // Clean input strings
        user.setEmail(tenant.getEmail().trim());
        user.setFirstName(tenant.getFirstName());
        user.setLastName(tenant.getLastName());
        user.setCredentials(Collections.singletonList(passwordCredential)); // Atomic binding
        user.setRequiredActions(Collections.emptyList());

        // to add these custom attributes you need to add them in keyclock relmSetting->userProfile->createAttribute
        /*user.setAttributes(Map.of(
                "tenant_id", Collections.singletonList(String.valueOf(tenant.getId())),
                "tenant_plan", Collections.singletonList(String.valueOf(tenant.getPlan())),
                "billing_status", Collections.singletonList(String.valueOf(tenant.getBillingStatus())),
                "timezone", Collections.singletonList(tenant.getTimezone() != null ? tenant.getTimezone() : "UTC")
        ));*/

        try (Keycloak keycloak = buildKeycloakClient()) {
            UsersResource usersResource = keycloak.realm(realm).users();

            try (Response response = usersResource.create(user)) {
                int status = response.getStatus();

                // Optimization 2: Extract explicit error reason body for 409 and other error states
                if (status != 201) {
                    String rawErrorJson = response.hasEntity() ? response.readEntity(String.class) : "No error body payload returned";

                    log.error("Keycloak registration rejected with HTTP status code: {}. Root cause body: {}", status, rawErrorJson);

                    if (status == 409) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "User identity profile details conflict: " + rawErrorJson);
                    }
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak automated user provisioning failed");
                }

                // Optimization 3: Safer ID parsing using the official client utility helper
                String createdUserId = org.keycloak.admin.client.CreatedResponseUtil.getCreatedId(response);
                log.info("User container saved securely. Keycloak internal UUID generated: {}, for tenant: {}", createdUserId, user.getUsername());
            }

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Fatal connectivity exception transmitting data over to Keycloak engine profile layer", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Identity provider sync breakdown", e);
        }
    }


    public void deprovisionKeycloakUser(String username) {
        String usernameToSearch = username.toLowerCase();
        log.info("Connecting to Keycloak Admin API target to delete user: {}", usernameToSearch);

        try (Keycloak keycloak = buildKeycloakClient()) {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> users = usersResource.searchByUsername(usernameToSearch, true);

            if (users.isEmpty()) {
                log.warn("No matching user found in Keycloak realm [{}] for username: {}", realm, usernameToSearch);
                return;
            }

            String keycloakUserId = users.get(0).getId();
            try (Response response = usersResource.delete(keycloakUserId)) {
                if (response.getStatus() != 204 && response.getStatus() != 200) {
                    log.error("Keycloak user deletion rejected with HTTP status code: {}", response.getStatus());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak automated user deprovisioning failed");
                }
                log.info("Identity user successfully purged from Keycloak realm [{}]: {}", realm, usernameToSearch);
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Fatal connectivity exception during Keycloak user deprovisioning layer", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Identity provider sync breakdown during deletion");
        }
    }
}
