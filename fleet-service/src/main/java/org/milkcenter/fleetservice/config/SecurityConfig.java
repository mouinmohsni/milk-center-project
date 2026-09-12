package org.milkcenter.fleetservice.config;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoint utilisé par un FARMER pour consulter ses propres arrêts.
                        // Il doit être placé avant les règles plus générales.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/route-stops/farmer/*"
                        ).hasRole("FARMER")

                        // Endpoint DRIVER personnel.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/drivers/me",
                                "/api/route-executions/driver/*"
                        ).hasRole("DRIVER")

                        // Lectures opérationnelles communes aux MANAGER et DRIVER.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/routes/*",
                                "/api/route-stops/route/*"
                        ).hasAnyRole("MANAGER", "DRIVER")

                        // Lecture d’un arrêt par son identifiant.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/route-stops/*"
                        ).hasAnyRole("MANAGER", "DRIVER", "FARMER")

                        // Lecture des exécutions par identifiant : accès opérationnel.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/route-executions/*"
                        ).hasAnyRole("MANAGER", "DRIVER")

                        // Lectures administratives réservées au MANAGER.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/drivers",
                                "/api/drivers/*",
                                "/api/drivers/user/*",
                                "/api/drivers/license/*",
                                "/api/drivers/status/*",
                                "/api/drivers/available",
                                "/api/drivers/salary",
                                "/api/routes",
                                "/api/routes/driver/*",
                                "/api/routes/vehicle/*",
                                "/api/routes/status/*",
                                "/api/vehicles",
                                "/api/vehicles/",
                                "/api/vehicles/*",
                                "/api/vehicles/license/*",
                                "/api/vehicles/status/*",
                                "/api/vehicles/model/*",
                                "/api/route-stops",
                                "/api/route-stops/",
                                "/api/route-stops/assignment-status/*",
                                "/api/route-executions",
                                "/api/route-executions/",
                                "/api/route-executions/route/*",
                                "/api/route-executions/vehicle/*"
                        ).hasRole("MANAGER")

                        // Création.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/drivers",
                                "/api/routes",
                                "/api/vehicles",
                                "/api/route-stops",
                                "/api/route-executions"
                        ).hasRole("MANAGER")

                        // Remplacement complet.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/drivers/*",
                                "/api/routes/*",
                                "/api/vehicles/*"
                        ).hasRole("MANAGER")

                        // Changements de statut et opérations administratives.
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

                        // Le DRIVER peut mettre à jour les opérations de son véhicule.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/vehicles/*/operations"
                        ).hasRole("DRIVER")

                        // Suppression.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/drivers/*",
                                "/api/routes/*",
                                "/api/vehicles/*",
                                "/api/route-stops/*",
                                "/api/route-executions/*"
                        ).hasRole("MANAGER")

                        // Modules administratifs.
                        .requestMatchers("/api/maintenances/**").hasRole("MANAGER")
                        .requestMatchers("/api/fuel-consumptions/**").hasRole("MANAGER")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authenticationException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"UNAUTHORIZED\","
                                            + "\"message\":\"Authentification requise\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"FORBIDDEN\","
                                            + "\"message\":\"Vous n'avez pas les autorisations nécessaires\"}"
                            );
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new KeycloakJwtAuthenticationConverter()
                        ))
                );

        return http.build();
    }
}
