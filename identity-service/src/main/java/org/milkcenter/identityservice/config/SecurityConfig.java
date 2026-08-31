package org.milkcenter.identityservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.milkcenter.identityservice.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder( ) {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService
    ) {
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider();

        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter
    ) throws Exception {

        http
                // API REST sans session et sans CSRF
                .csrf(csrf -> csrf.disable( ))

                .authorizeHttpRequests(auth -> auth

                        // Inscription et connexion accessibles sans token
                        .requestMatchers("/api/auth/**").permitAll()

                        // Consultation de son propre profil
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        ).authenticated()

                        // Modification de son propre profil
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/users/me"
                        ).authenticated()

                        // Modification du rôle : MANAGER uniquement
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/users/*/role"
                        ).hasRole("MANAGER")

                        // Modification complète d'un autre utilisateur : MANAGER uniquement
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/*"
                        ).hasRole("MANAGER")

                        // Suppression d'un utilisateur : MANAGER uniquement
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/*/hard",
                                "/api/users/*"
                        ).hasRole("MANAGER")

                        // Liste et consultation des utilisateurs : MANAGER uniquement
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users",
                                "/api/users/",
                                "/api/users/*"
                        ).hasRole("MANAGER")

                        // Toute autre requête nécessite un utilisateur authentifié
                        .anyRequest().authenticated()
                )

                // Gestion des erreurs générées directement par Spring Security
                .exceptionHandling(exception -> exception

                        // Aucun token ou token invalide
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

                        // Token valide mais rôle insuffisant
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

                // Exécution du filtre JWT avant le filtre standard de Spring Security
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build( );
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
