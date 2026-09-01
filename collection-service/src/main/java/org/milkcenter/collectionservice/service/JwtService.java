package org.milkcenter.collectionservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Reconstruit la clé utilisée pour vérifier la signature du JWT.
     * La valeur doit être identique à celle utilisée par identity-service.
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extrait username stocké dans le subject du token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait un claim générique du JWT.
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrait le rôle stocké dans le claim "role".
     * Exemple : MANAGER ou ROLE_MANAGER.
     */
    public String extractRole(String token) {
        return extractClaim(
                token,
                claims -> claims.get("role", String.class)
        );
    }

    /**
     * Vérifie la signature et lit le contenu du token.
     * Une exception signifie que le token est invalide ou expiré.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Vérifie que le token est valide et qu'il n'est pas expiré.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();

            return expiration != null
                    && expiration.after(new Date());

        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Number userId = claims.get("userId", Number.class);
            return userId != null ? userId.longValue() : null;
        });
    }

}
