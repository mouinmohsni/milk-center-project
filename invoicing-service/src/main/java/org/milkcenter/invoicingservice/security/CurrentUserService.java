package org.milkcenter.invoicingservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "Aucun utilisateur Keycloak authentifié"
            );
        }

        Jwt jwt = jwtAuthentication.getToken();

        String userIdClaim = jwt.getClaimAsString("userId");

        if (userIdClaim == null || userIdClaim.isBlank()) {
            throw new IllegalStateException(
                    "Le claim userId est absent du token Keycloak"
            );
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdClaim);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Le claim userId n'est pas un identifiant numérique valide",
                    exception
            );
        }

        String username = jwt.getClaimAsString("preferred_username");

        if (username == null || username.isBlank()) {
            username = jwt.getSubject();
        }

        String role = jwtAuthentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .filter(this::isBusinessRole)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun rôle métier FARMER, DRIVER ou MANAGER dans le token Keycloak"
                ));

        return new AuthenticatedUser(
                userId,
                username,
                role
        );
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    public String getCurrentRole() {
        return getCurrentUser().getRole();
    }

    private boolean isBusinessRole(String role) {
        return "FARMER".equals(role)
                || "DRIVER".equals(role)
                || "MANAGER".equals(role);
    }
}
