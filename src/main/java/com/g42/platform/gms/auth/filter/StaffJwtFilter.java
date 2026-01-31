package com.g42.platform.gms.auth.filter;

import com.g42.platform.gms.auth.service.JWTService;
import com.g42.platform.gms.auth.service.StaffAuthDetailsService;
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
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();


        if (path.startsWith("/api/auth/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            //decoding jwt get username
            String username = jwtService.extractUserName(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                //load user from database check staff exist in db and acc status
                UserDetails userDetails = context.getBean(StaffAuthDetailsService.class).loadUserByUsername(username);
                //validate token
                if (jwtService.validateToken(token,userDetails)){
                    //create authentication obj
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken
                            (userDetails,null,userDetails.getAuthorities());
                    //attach request metadata contain ip, sessionId
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    //save authen to secirityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
    }
}
}
