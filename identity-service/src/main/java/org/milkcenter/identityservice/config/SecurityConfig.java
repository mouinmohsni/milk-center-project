package org.milkcenter.identityservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Utilisé uniquement pour encoder le mot de passe local avant
     * sa synchronisation avec Keycloak.
     */
    @Bean
    public PasswordEncoder passwordEncoder( ) {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable( ))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/users/me"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/users/*/role"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/*"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/users/*/hard",
                                "/api/users/*"
                        ).hasRole("MANAGER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users",
                                "/api/users/",
                                "/api/users/*"
                        ).hasRole("MANAGER")

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

                // Validation des tokens OIDC/JWT émis par Keycloak
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );


        return http.build( );
    }
}
