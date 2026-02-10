package com.g42.platform.gms.security.filter;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.service.JWTService;
import com.g42.platform.gms.auth.service.StaffAuthDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class StaffJwtFilter extends OncePerRequestFilter {

    @Autowired
    private JWTService jwtService;
    @Autowired
    ApplicationContext context;
    @Autowired
    StaffAuthRepo staffAuthRepo;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // bỏ qua các endpoint không cần parse staff token
        // - /api/auth/**: auth endpoints (login, register) - không cần token
        // - /api/booking/guest/**: guest booking - public endpoint
        // - /api/booking/customer/**: customer endpoints - dùng customer token
        // - swagger, home, error: public endpoints
        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/booking/guest/")
                || path.startsWith("/api/booking/customer/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-ui.html")
                || path.startsWith("/home")
                || path.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // kiểm tra có authorization header không
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // extract token từ header
                String token = authHeader.substring(7);
                
                // decode jwt để lấy subject (staff auth id)
                String subject = jwtService.extractUserName(token);
                
                // load staff từ database để kiểm tra tồn tại và trạng thái
                StaffAuth staffauth = staffAuthRepo.searchByStaffAuthId(Long.parseLong(subject));
                if (staffauth == null) {
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // chỉ set authentication nếu chưa có authentication trong security context
                if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // tạo staff principal từ staff auth
                    StaffPrincipal staffPrincipal = new StaffPrincipal(staffauth);
                    
                    // validate token
                    if (jwtService.validateToken(token)) {
                        // tạo authentication object với authorities từ staff principal
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken
                                (staffPrincipal, null, staffPrincipal.getAuthorities());
                        
                        // attach request metadata (ip, sessionId)
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // log thông tin request (có thể dùng để audit)
                        String userAgent = request.getHeader("User-Agent");
                        String ip = request.getHeader("X-Forwarded-For");
                        System.out.println("WEB: " + userAgent);
                        System.out.println("IP: " + ip);
                        
                        // set authentication vào security context để authorization sử dụng
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            // token đã hết hạn
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            {
              "error": "JWT_EXPIRED",
              "message": "Token expired"
            }
        """);
        } catch (JwtException e) {
            // token không hợp lệ
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            {
              "error": "INVALID_JWT",
              "message": "Token not valid"
            }
        """);
        }
    }
}
