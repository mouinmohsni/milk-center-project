package org.milkcenter.invoicingservice.config;

import jakarta.servlet.http.HttpServletResponse;
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
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter
    ) throws Exception {

        http
                // API REST sans session et sans formulaire web.
                .csrf(csrf -> csrf.disable( ))
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable( ))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // =====================================================
                        // DRIVER : LECTURE DES RESSOURCES QUI PEUVENT LE CONCERNER
                        // =====================================================
                        // Les services doivent ensuite vérifier l'affectation réelle.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/drivers/me",
                                "/api/routes/*",
                                "/api/route-stops/*",
                                "/api/route-stops/route/*",
                                "/api/route-stops/farmer/*",
                                "/api/route-executions/*",
                                "/api/route-executions/driver/*"
                        ).hasRole("DRIVER")

                        // =====================================================
                        // FARMER : LECTURE DE SES PROPRES ARRETS
                        // =====================================================
                        // RouteStopService doit vérifier que farmerId correspond
                        // bien à l'utilisateur présent dans le JWT.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/route-stops/*",
                                "/api/route-stops/farmer/*"
                        ).hasRole("FARMER")

                        // =====================================================
                        // DRIVER ET MANAGER : STATUT D'UNE EXECUTION
                        // =====================================================
                        // RouteExecutionService doit vérifier que le DRIVER
                        // est le chauffeur réellement affecté à l'exécution.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/route-executions/*/status"
                        ).hasAnyRole("MANAGER", "DRIVER")

                        // =====================================================
                        // MANAGER : LECTURES ADMINISTRATIVES
                        // =====================================================
                        .requestMatchers(
                                HttpMethod.GET,
                                // Drivers
                                "/api/drivers",
                                "/api/drivers/*",
                                "/api/drivers/user/*",
                                "/api/drivers/license/*",
                                "/api/drivers/status/*",
                                "/api/drivers/available",
                                "/api/drivers/salary",

                                // Routes
                                "/api/routes",
                                "/api/routes/",
                                "/api/routes/driver/*",
                                "/api/routes/vehicle/*",
                                "/api/routes/status/*",

                                // Véhicules
                                "/api/vehicles",
                                "/api/vehicles/",
                                "/api/vehicles/*",
                                "/api/vehicles/license/*",
                                "/api/vehicles/status/*",
                                "/api/vehicles/model/*",

                                // Route stops globaux
                                "/api/route-stops",
                                "/api/route-stops/",
                                "/api/route-stops/assignment-status/*",

                                // Route executions globales
                                "/api/route-executions",
                                "/api/route-executions/",
                                "/api/route-executions/route/*",
                                "/api/route-executions/vehicle/*"
                        ).hasRole("MANAGER")

                        // =====================================================
                        // MANAGER : CREATION
                        // =====================================================
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/drivers",
                                "/api/routes",
                                "/api/vehicles",
                                "/api/route-stops",
                                "/api/route-executions"
                        ).hasRole("MANAGER")

                        // =====================================================
                        // MANAGER : REMPLACEMENT COMPLET
                        // =====================================================
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/drivers/*",
                                "/api/routes/*",
                                "/api/vehicles/*"
                        ).hasRole("MANAGER")

                        // =====================================================
                        // MANAGER : MODIFICATIONS ADMINISTRATIVES
                        // =====================================================
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/drivers/*/status",
                                "/api/routes/*/status",
                                "/api/routes/*/activate",
                                "/api/routes/*/cancel",
                                "/api/vehicles/*/status",
                                "/api/route-stops/*/assign",
                                "/api/route-stops/*/unassign",
                                "/api/route-stops/*",
                                "/api/route-executions/*"
                        ).hasRole("MANAGER")

                        // =====================================================
                        // DRIVER : OPERATIONS DU VEHICULE
                        // =====================================================
                        // VehiculeService doit vérifier que le véhicule est
                        // affecté au DRIVER via une RouteExecution active.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/vehicles/*/operations"
                        ).hasRole("DRIVER")

                        // =====================================================
                        // MANAGER : SUPPRESSION
                        // =====================================================
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/drivers/*",
                                "/api/routes/*",
                                "/api/vehicles/*",
                                "/api/route-stops/*",
                                "/api/route-executions/*"
                        ).hasRole("MANAGER")

                        // Toute autre requête doit être authentifiée.
                        .anyRequest().authenticated()
                )

                // Réponses JSON pour 401 et 403.
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

                // Le filtre JWT doit être exécuté avant le filtre standard.
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build( );
    }
}
