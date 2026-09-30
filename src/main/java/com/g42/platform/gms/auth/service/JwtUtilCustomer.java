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
import java.util.Optional;

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

    /**
     * Claims của token khách còn hạn; rỗng nếu không phải token khách hợp lệ.
     *
     * <p>Mọi request có Bearer token đều đi qua filter khách trước filter nhân viên, nên token
     * nhân viên (ký bằng key khác — JWTService base64-decode secret, lớp này thì không) sẽ luôn
     * sai chữ ký ở đây. Đó là trường hợp BÌNH THƯỜNG, chỉ log DEBUG; trước đây log ERROR cho
     * từng request của nhân viên khiến log đầy "Secret key mismatch" dù request vẫn thành công.
     */
    public Optional<Claims> parseValidClaims(String token) {
        try {
            return Optional.of(extractClaims(token));
        } catch (ExpiredJwtException e) {
            log.debug("Customer token expired: exp={}", e.getClaims().getExpiration());
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.debug("Không phải token khách (sai chữ ký) — để filter nhân viên xử lý");
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Token malformed/unsupported: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return Optional.empty();
    }
}
