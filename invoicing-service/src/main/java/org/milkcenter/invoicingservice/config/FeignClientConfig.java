package org.milkcenter.invoicingservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration(proxyBeanMethods = false )
public class FeignClientConfig {

    private static final String CLIENT_REGISTRATION_ID = "service-client";
    private static final String TECHNICAL_PRINCIPAL = "invoicing-service";

    /**
     * Gestionnaire utilisé uniquement pour obtenir un token technique
     * avec le grant client_credentials.
     */
    @Bean
    public OAuth2AuthorizedClientManager oauth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );

        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }

    /**
     * Intercepteur hybride :
     *
     * - appel manuel avec un MANAGER : propagation du token entrant ;
     * - appel automatique sans requête HTTP : token technique AUTOSERVICE.
     */
    @Bean
    public RequestInterceptor authenticationRequestInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            /* Appel manuel provenant d'une requête HTTP. */
            if (attributes != null) {
                HttpServletRequest currentRequest = attributes.getRequest();
                String incomingAuthorization =
                        currentRequest.getHeader(HttpHeaders.AUTHORIZATION);

                Authentication authentication =
                        SecurityContextHolder.getContext().getAuthentication();

                boolean isManager = authentication != null
                        && authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                "ROLE_MANAGER".equals(authority.getAuthority()));

                if (!isManager) {
                    throw new AccessDeniedException(
                            "Un appel manuel Feign nécessite le rôle MANAGER"
                    );
                }

                if (incomingAuthorization == null
                        || !incomingAuthorization.startsWith("Bearer ")) {
                    throw new AccessDeniedException(
                            "Le token utilisateur est absent de la requête"
                    );
                }

                requestTemplate.header(
                        HttpHeaders.AUTHORIZATION,
                        incomingAuthorization
                );
                return;
            }

            /* Appel automatique provenant du scheduler ou d'un traitement en arrière-plan. */
            OAuth2AuthorizeRequest authorizeRequest =
                    OAuth2AuthorizeRequest
                            .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                            .principal(TECHNICAL_PRINCIPAL)
                            .build();

            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null
                    || authorizedClient.getAccessToken() == null
                    || authorizedClient.getAccessToken().getTokenValue() == null) {
                throw new IllegalStateException(
                        "Impossible d'obtenir le token OAuth2 pour le client "
                                + CLIENT_REGISTRATION_ID
                );
            }

            requestTemplate.header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + authorizedClient.getAccessToken().getTokenValue()
            );
        };
    }
}
