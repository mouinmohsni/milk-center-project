package org.milkcenter.invoicingservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable( ))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Consultation de ses propres factures.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/invoices/me"
                        ).hasAnyRole("FARMER", "MANAGER")

                        // Consultation d'une facture précise.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/invoices/*"
                        ).hasAnyRole("FARMER", "MANAGER")

                        // Liste globale et recherche par fermier.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/invoices",
                                "/api/invoices/",
                                "/api/invoices/farmer/*"
                        ).hasRole("MANAGER")

                        // Création d'une facture.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/invoices"
                        ).hasRole("MANAGER")

                        // Modification et changement de statut.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/invoices/*",
                                "/api/invoices/*/status"
                        ).hasRole("MANAGER")

                        // Suppression d'une facture.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/invoices/*"
                        ).hasRole("MANAGER")

                        // Consultation des paiements.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/payments/*",
                                "/api/payments/invoice/*"
                        ).hasAnyRole("FARMER", "MANAGER")

                        // Création d'un paiement.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/payments/invoice/*"
                        ).hasRole("MANAGER")

                        // Modification d'un paiement.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/payments/*",
                                "/api/payments/*/status"
                        ).hasRole("MANAGER")

                        // Suppression d'un paiement.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/payments/*"
                        ).hasRole("MANAGER")

                        // Création d'une configuration tarifaire.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/pricing-configurations"
                        ).hasRole("MANAGER")

                        // Consultation des configurations tarifaires.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pricing-configurations",
                                "/api/pricing-configurations/*",
                                "/api/pricing-configurations/type/*"
                        ).hasRole("MANAGER")

                        // Modification d'une configuration tarifaire.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/pricing-configurations/*"
                        ).hasRole("MANAGER")

                        // Suppression d'une configuration tarifaire.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/pricing-configurations/*",
                                "/api/pricing-configurations/*/hard"
                        ).hasRole("MANAGER")

                        // Toutes les autres routes nécessitent un token Keycloak valide.
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                    {
                                      "status": 401,
                                      "error": "UNAUTHORIZED",
                                      "message": "Authentification requise"
                                    }
                                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                    {
                                      "status": 403,
                                      "error": "FORBIDDEN",
                                      "message": "Vous n'avez pas les autorisations nécessaires"
                                    }
                                    """);
                        })
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new KeycloakJwtAuthenticationConverter()
                        ))
                );

        return http.build( );
    }
}
