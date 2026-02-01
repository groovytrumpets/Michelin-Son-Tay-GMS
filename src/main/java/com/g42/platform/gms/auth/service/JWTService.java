package com.g42.platform.gms.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {
    @Value("${jwt.secret}") String secretKey;
    @Value("${jwt.expirationMs}") long expirationTime;

    public String generateStaffJWToken(Long authId) {

        Map<String, Object> claims = new HashMap<>();
        return Jwts
                .builder()
                .setClaims(claims)
                .addClaims(claims)
                .setSubject(String.valueOf(authId)) //phone or email
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+expirationTime))
                .signWith(getKey())
                .compact();
    }

    private Key getKey() {
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsExtractor) {
        final Claims claims = extractAllClaims(token);
        return claimsExtractor.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
        return (!isTokenExpired(token));
        }catch (JwtException e){
            return false;
        }

    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
