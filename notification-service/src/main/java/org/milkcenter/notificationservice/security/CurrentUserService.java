package org.milkcenter.notificationservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public Long userId() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(p instanceof Jwt jwt)) throw new IllegalStateException("JWT absent");
        Object value = jwt.getClaims().get("userId");
        if (value == null) value = jwt.getSubject();
        return Long.valueOf(String.valueOf(value));
    }
}
