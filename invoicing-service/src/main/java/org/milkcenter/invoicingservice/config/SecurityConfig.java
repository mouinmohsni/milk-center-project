package org.milkcenter.invoicingservice.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.milkcenter.invoicingservice.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable( ))
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable( ))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Il n'y a pas de /api/auth/** dans invoicing-service.
                         * Le login est géré par identity-service.
                         */

                        // Consultation de ses propres factures.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/invoices/me"
                        ).hasAnyRole("FARMER", "MANAGER")

                        // Consultation d'une facture précise.
                        // Le service vérifie ensuite la propriété du fermier.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/invoices/*"
                        ).hasAnyRole("FARMER", "MANAGER")

                        // Liste globale et recherche par fermier : MANAGER uniquement.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/invoices",
                                "/api/invoices/",
                                "/api/invoices/farmer/*"
                        ).hasRole("MANAGER")

                        // Création, modification, changement de statut et suppression.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/invoices"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/invoices/*",
                                "/api/invoices/*/status"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/invoices/*"
                        ).hasRole("MANAGER")

                        // Consultation de ses paiements ou des paiements d'une facture accessible.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/payments/*",
                                "/api/payments/invoice/*"
                        ).hasAnyRole("FARMER", "MANAGER")

                        // Création et modification des paiements : MANAGER uniquement.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/payments/invoice/*"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/payments/*",
                                "/api/payments/*/status"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/payments/*"
                        ).hasRole("MANAGER")

                        // Toute autre route nécessite un JWT valide.
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, exceptionAuth) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,"
                                            + "\"error\":\"UNAUTHORIZED\","
                                            + "\"message\":\"Authentification requise\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":403,"
                                            + "\"error\":\"FORBIDDEN\","
                                            + "\"message\":\"Vous n'avez pas les autorisations nécessaires\"}"
                            );
                        })
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build( );
    }
}
