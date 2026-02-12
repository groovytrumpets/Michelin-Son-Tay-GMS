package com.g42.platform.gms.security.filter;

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

        String path = request.getRequestURI();
        
        // bỏ qua các endpoint không cần parse customer token
        // - /api/auth/**: auth endpoints (login, register) - không cần token
        // - /api/booking/guest/**: guest booking - public endpoint
        // - /api/staff-profile, /api/book/handle: staff endpoints - dùng staff token
        // - swagger, home, error: public endpoints
        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/booking/guest/")
                || path.startsWith("/api/booking/slots/")
                || path.startsWith("/api/staff-profile")
                || path.startsWith("/api/book/handle")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-ui.html")
                || path.startsWith("/home")
                || path.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        // kiểm tra có authorization header không
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Authorization header for path: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // extract token từ header
        final String token = authHeader.substring(7).trim();
        
        try {
            // validate customer token
            boolean isValid = jwtUtilCustomer.isTokenValid(token);
            log.debug("JWT Filter: Token validation result for {}: {}", request.getRequestURI(), isValid);

            if (isValid) {
                // parse claims từ token
                Claims claims = jwtUtilCustomer.extractClaims(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                Integer customerId = claims.get("customerId", Integer.class);

                // tạo authorities từ role trong token
                List<SimpleGrantedAuthority> authorities = (role == null)
                        ? Collections.emptyList()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // tạo authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                // attach request metadata (ip, sessionId)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // set authentication vào security context để authorization sử dụng
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("JWT Authentication successful - User: {}, Role: {}, Path: {}", username, role, request.getRequestURI());
            } else {
                log.warn("JWT Token validation failed for path: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            // nếu parse lỗi (có thể là staff token hoặc token không hợp lệ) → catch và cho qua
            // staff token sẽ được xử lý bởi StaffJwtFilter
            log.debug("Token không phải customer token hoặc lỗi parse: {} - {}", 
                e.getClass().getSimpleName(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}