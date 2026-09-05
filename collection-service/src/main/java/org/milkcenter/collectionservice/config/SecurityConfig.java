package org.milkcenter.collectionservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.milkcenter.collectionservice.filter.JwtAuthFilter;
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
                .csrf(csrf -> csrf.disable( ))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // --- PROFILS FERMIERS (FARMER PROFILES) ---

                        // Accès personnel (Priorité haute)
                        .requestMatchers(HttpMethod.GET, "/api/farmers/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/farmers/me").authenticated()

                        // Création : MANAGER ou le futur FARMER lui-même
                        .requestMatchers(HttpMethod.POST, "/api/farmers")
                        .hasAnyRole("MANAGER", "FARMER")

                        // Consultation (DRIVER en a besoin pour identifier les fermes sur sa route)
                        .requestMatchers(HttpMethod.GET, "/api/farmers/**")
                        .hasAnyRole("MANAGER", "DRIVER")

                        // Gestion administrative
                        .requestMatchers(HttpMethod.PUT, "/api/farmers/*")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/farmers/*/deactivate")
                        .hasRole("MANAGER")


                        // --- COLLECTES DE LAIT (MILK COLLECTIONS) ---

                        // Accès personnels
                        .requestMatchers(HttpMethod.GET, "/api/collections/driver/me").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/collections/farmer/me").hasRole("FARMER")

                        // Opérations Chauffeur
                        .requestMatchers(HttpMethod.POST, "/api/collections").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/collections/route-stop/*").hasAnyRole("MANAGER", "DRIVER")

                        // Validation (Le DRIVER peut valider/corriger si autorisé par le métier)
                        .requestMatchers(HttpMethod.PUT, "/api/collections/*/validate").hasAnyRole("MANAGER", "DRIVER")

                        // Statistiques (Le FARMER peut voir ses propres stats)
                        .requestMatchers("/api/collections/stats/**").hasAnyRole("MANAGER", "FARMER")

                        // Total mensuel ACCEPTED utilisé pour la facturation
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/collections/farmer/*/monthly-total"
                        )
                        .hasRole("MANAGER")


                        // Consultation globale (Réservée au MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/collections/**").hasRole("MANAGER")


                        // Toute autre API nécessite une authentification
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"status\": 401, \"error\": \"UNAUTHORIZED\", \"message\": \"Authentification requise\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"status\": 403, \"error\": \"FORBIDDEN\", \"message\": \"Vous n'avez pas les autorisations nécessaires\"}");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build( );
    }
}
