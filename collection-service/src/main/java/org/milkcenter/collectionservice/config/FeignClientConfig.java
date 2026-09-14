package org.milkcenter.collectionservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
    private static final String TECHNICAL_PRINCIPAL = "collection-service";

    /**
     * Gestionnaire utilisé pour les appels automatiques sans requête HTTP.
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
     * Intercepteur hybride de collection-service.
     *
     * Appel manuel : transmet le token de l'utilisateur courant,
     * qu'il soit DRIVER ou MANAGER.
     *
     * Appel automatique : obtient un token client_credentials.
     */
    @Bean
    public RequestInterceptor authenticationRequestInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            /* Appel provenant d'une requête HTTP utilisateur. */
            if (attributes != null) {
                HttpServletRequest currentRequest = attributes.getRequest();
                String incomingAuthorization =
                        currentRequest.getHeader(HttpHeaders.AUTHORIZATION);

                if (incomingAuthorization == null
                        || !incomingAuthorization.startsWith("Bearer ")) {
                    throw new IllegalStateException(
                            "Le token utilisateur est absent de la requête"
                    );
                }

                requestTemplate.header(
                        HttpHeaders.AUTHORIZATION,
                        incomingAuthorization
                );
                return;
            }

            /* Appel automatique sans requête HTTP. */
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
