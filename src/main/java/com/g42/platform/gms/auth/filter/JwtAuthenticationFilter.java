package com.g42.platform.gms.auth.filter;

import com.g42.platform.gms.auth.service.JwtUtilCustomer;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtilCustomer jwtUtilCustomer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String token;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Authorization header for path: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        token = authHeader.substring(7).trim();
        
        try {
            boolean isValid = jwtUtilCustomer.isTokenValid(token);
            log.debug("JWT Filter: Token validation result for {}: {}", request.getRequestURI(), isValid);

            if (isValid) {
                Claims claims = jwtUtilCustomer.extractClaims(token);
                username = claims.getSubject();
                String role = claims.get("role", String.class);
                Integer customerId = claims.get("customerId", Integer.class);

                List<SimpleGrantedAuthority> authorities = (role == null)
                        ? Collections.emptyList()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("JWT Authentication successful - User: {}, Role: {}, Path: {}", username, role, request.getRequestURI());
            } else {
                log.warn("JWT Token validation failed for path: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("JWT Authentication error for path {}: {} - {}", 
                request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}