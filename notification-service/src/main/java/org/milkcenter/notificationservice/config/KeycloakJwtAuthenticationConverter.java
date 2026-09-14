package org.milkcenter.notificationservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(
                jwt,
                extractRealmRoles(jwt),
                getPrincipalName(jwt)
        );
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Object realmAccessObject = jwt.getClaims().get("realm_access");



        if (!(realmAccessObject instanceof Map<?, ?> realmAccess)) {
            return Collections.emptyList();
        }

        Object rolesObject = realmAccess.get("roles");

        if (!(rolesObject instanceof List<?> rolesList)) {
            return Collections.emptyList();
        }
        System.out.println("realmAccessObject =======> " + realmAccessObject);
        System.out.println("rolesObject =======> " + rolesObject);

        return rolesList.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_")
                        ? role
                        : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
    }

    private String getPrincipalName(Jwt jwt) {
        String preferredUsername =
                jwt.getClaimAsString("preferred_username");

        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }

        return jwt.getSubject();
    }
}
