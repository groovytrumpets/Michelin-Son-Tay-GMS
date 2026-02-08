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
@Component // 1. BẮT BUỘC: Để Spring quản lý Bean này
@RequiredArgsConstructor // Tự tạo Constructor cho jwtUtil
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtilCustomer jwtUtilCustomer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 2. Bỏ đoạn check path thủ công (/api/auth...).
        // Lý do: SecurityConfig đã cấu hình permitAll() rồi. Filter này cứ chạy,
        // nếu không có token thì nó cho qua (user = null), SecurityConfig sẽ chặn sau.

        final String authHeader = request.getHeader("Authorization");
        final String token;
        final String username;

        // Kiểm tra header: Nếu không có hoặc sai định dạng -> Cho qua (để API public chạy tiếp)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        token = authHeader.substring(7); // Cắt bỏ chữ "Bearer "

        try {
            // Kiểm tra Token hợp lệ
            if (jwtUtilCustomer.isTokenValid(token)) {
                Claims claims = jwtUtilCustomer.extractClaims(token);
                username = claims.getSubject(); // Lấy số điện thoại hoặc username

                // 3. XỬ LÝ PHÂN QUYỀN (QUAN TRỌNG)
                // Lấy claim "role" mà bạn đã put vào lúc login (VD: "CUSTOMER" hoặc "STAFF")
                String role = claims.get("role", String.class);

                // Spring Security yêu cầu Role nên có prefix "ROLE_"
                List<SimpleGrantedAuthority> authorities = (role == null)
                        ? Collections.emptyList()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // Tạo đối tượng Authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities // Nạp quyền vào đây để dùng hasRole()
                        );

                // Thêm thông tin request (IP, Session ID...) vào auth
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Lưu vào SecurityContext để Spring biết User này đã đăng nhập
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Nếu Token lỗi (hết hạn, sai chữ ký...), ta chỉ log và không set Authentication.
            // Request vẫn đi tiếp, nhưng sẽ bị chặn ở SecurityConfig với lỗi 403/401.
            log.error("JWT Authentication failed: {}", e.getMessage());
        }

        // Chuyển tiếp request cho filter tiếp theo
        filterChain.doFilter(request, response);
    }
}