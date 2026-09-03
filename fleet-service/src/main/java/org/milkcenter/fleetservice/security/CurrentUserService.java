package org.milkcenter.fleetservice.security;

import org.milkcenter.fleetservice.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
            throw new IllegalStateException(
                    "Aucun utilisateur authentifié"
            );
        }

        return (AuthenticatedUser) authentication.getPrincipal();
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
}
