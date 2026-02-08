package com.g42.platform.gms.auth.filter;

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


        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/booking/guest/") // Thêm dòng này
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-ui.html")){
            filterChain.doFilter(request, response);
            return;
        }
        try{


        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            //decoding jwt get username
            String subject = jwtService.extractUserName(token);
            StaffAuth staffauth = staffAuthRepo.searchByStaffAuthId(Long.parseLong(subject));
            if (staffauth == null) {
                filterChain.doFilter(request, response);
                return;
            }
            //System.out.println( "USERNAME: "+username);
            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                //load user from database check staff exist in db and acc status
                StaffPrincipal  staffPrincipal = new StaffPrincipal(staffauth);
                //validate token
                if (jwtService.validateToken(token)){
                    //create authentication obj
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken
                            (staffPrincipal,null,staffPrincipal.getAuthorities());
                    //attach request metadata contain ip, sessionId
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    String userAgent = request.getHeader("User-Agent");
                    String ip = request.getHeader("X-Forwarded-For");
                    System.out.println("WEB: "+userAgent);
                    System.out.println("IP: "+ip);
                    //save authen to secirityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
    }
        }catch (ExpiredJwtException e){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            {
              "error": "JWT_EXPIRED",
              "message": "Token expired"
            }
        """);
        }catch (JwtException e){
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
