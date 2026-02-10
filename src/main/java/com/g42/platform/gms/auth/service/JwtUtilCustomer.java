package com.g42.platform.gms.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtilCustomer {

    private final Key key;
    private final long expirationMs;

    public JwtUtilCustomer(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expirationMs}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            if (expiration.before(now)) {
                log.error("Token expired: exp={}, now={}", expiration, now);
                return false;
            }
            log.debug("Token is valid: exp={}, now={}", expiration, now);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Token expired: exp={}, now={}", e.getClaims().getExpiration(), new Date());
            return false;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("Token signature invalid - Secret key mismatch? Error: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Token malformed: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Token unsupported: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}
