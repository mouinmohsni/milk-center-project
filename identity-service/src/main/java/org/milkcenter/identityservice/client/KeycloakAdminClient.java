package org.milkcenter.identityservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private static final String REGISTRATION_ID = "keycloak-admin";
    private static final String PRINCIPAL_NAME = "identity-service";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    @Value("${keycloak.admin.base-url}" )
    private String keycloakAdminBaseUrl;

    public String getAdminAccessToken() {
        OAuth2AuthorizeRequest authorizeRequest =
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(REGISTRATION_ID)
                        .principal(PRINCIPAL_NAME)
                        .build();

        OAuth2AuthorizedClient authorizedClient =
                authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient == null
                || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Impossible d'obtenir un access token Keycloak pour identity-service"
            );
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    public String createUser(KeycloakCreateUserRequest request) {
        String accessToken = getAdminAccessToken();

        RestClient restClient = RestClient.builder()
                .baseUrl(keycloakAdminBaseUrl)
                .build();

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();

            if (location == null || location.getPath() == null) {
                throw new IllegalStateException(
                        "Keycloak a créé l'utilisateur mais n'a pas retourné son identifiant"
                );
            }

            String path = location.getPath();
            String keycloakUserId = path.substring(path.lastIndexOf('/') + 1);

            if (keycloakUserId.isBlank()) {
                throw new IllegalStateException(
                        "L'identifiant Keycloak retourné est vide"
                );
            }

            return keycloakUserId;

        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new IllegalStateException(
                        "Un utilisateur avec ce username ou cet email existe déjà dans Keycloak",
                        exception
                );
            }

            throw new IllegalStateException(
                    "Erreur lors de la création de l'utilisateur dans Keycloak. Statut : "
                            + exception.getStatusCode().value(),
                    exception
            );
        }
    }

    public void assignRealmRole(String keycloakUserId, String roleName) {
        String accessToken = getAdminAccessToken();

        RestClient restClient = RestClient.builder()
                .baseUrl(keycloakAdminBaseUrl)
                .build();

        try {
            KeycloakRoleRepresentation role = restClient.get()
                    .uri("/roles/{roleName}", roleName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KeycloakRoleRepresentation.class);

            if (role == null || role.getId() == null || role.getName() == null) {
                throw new IllegalStateException(
                        "Le rôle Keycloak " + roleName + " est introuvable"
                );
            }

            restClient.post()
                    .uri("/users/{userId}/role-mappings/realm", keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.List.of(role))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Impossible d'attribuer le rôle " + roleName
                            + " à l'utilisateur Keycloak. Statut : "
                            + exception.getStatusCode().value(),
                    exception
            );
        }
    }
}
