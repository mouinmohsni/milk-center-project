package org.milkcenter.invoicingservice.config;


import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;

@Configuration(proxyBeanMethods = false)
public class FeignClientConfig {

    private static final String CLIENT_REGISTRATION_ID = "service-client";
    private static final String TECHNICAL_PRINCIPAL = "invoicing-service";

    /**
     * OAuth2 manager capable of obtaining a client_credentials token even when
     * the call is made by a scheduler and no HTTP request is available.
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
     * Adds a Keycloak access token to every request sent by this Feign client.
     * The token is obtained from the registration named service-client.
     */
    @Bean
    public RequestInterceptor oauth2RequestInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return requestTemplate -> {
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
                    "Authorization",
                    "Bearer " + authorizedClient.getAccessToken().getTokenValue()
            );
        };
    }
}
