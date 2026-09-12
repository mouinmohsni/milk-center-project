package org.milkcenter.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.milkcenter.identityservice.dto.request.UserLoginRequest;
import org.milkcenter.identityservice.dto.request.UserRegisterRequest;
import org.milkcenter.identityservice.dto.response.UserResponse;
import org.milkcenter.identityservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth" )
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${keycloak.oidc.token-uri}")
    private String keycloakTokenUri;

    @Value("${keycloak.oidc.logout-uri}")
    private String keycloakLogoutUri;

    @Value("${keycloak.oidc.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.oidc.client-secret:}")
    private String keycloakClientSecret;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody UserLoginRequest request
    ) {
        RestClient restClient = RestClient.builder().build();

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("grant_type", "password");
        formData.add("client_id", keycloakClientId);
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());
        formData.add("scope", "openid profile email");

        if (keycloakClientSecret != null
                && !keycloakClientSecret.isBlank()) {
            formData.add("client_secret", keycloakClientSecret);
        }

        try {
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(keycloakTokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse == null || tokenResponse.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "AUTHENTICATION_FAILED",
                                "message", "Keycloak n'a retourné aucun token"
                        ));
            }

            return ResponseEntity.ok(tokenResponse);

        } catch (RestClientResponseException exception) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "AUTHENTICATION_FAILED",
                            "message", "Identifiants incorrects ou compte désactivé"
                    ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody Map<String, String> request
    ) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", "INVALID_REQUEST",
                            "message", "Le refreshToken est obligatoire"
                    ));
        }

        RestClient restClient = RestClient.builder().build();

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("client_id", keycloakClientId);
        formData.add("refresh_token", refreshToken);

        if (keycloakClientSecret != null
                && !keycloakClientSecret.isBlank()) {
            formData.add("client_secret", keycloakClientSecret);
        }

        try {
            restClient.post()
                    .uri(keycloakLogoutUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            return ResponseEntity.ok(
                    Map.of("message", "Déconnexion réussie")
            );

        } catch (RestClientResponseException exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "error", "LOGOUT_FAILED",
                            "message", "Keycloak n'a pas pu terminer la déconnexion"
                    ));
        }
    }
}
