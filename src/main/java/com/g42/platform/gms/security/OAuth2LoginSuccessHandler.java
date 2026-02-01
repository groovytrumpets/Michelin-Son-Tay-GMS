package com.g42.platform.gms.security;

import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@AllArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTService  jwtService;
    private final StaffAuthRepo staffAuthRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttribute("email");
        String googleId = (String) oAuth2User.getAttribute("sub");

        StaffAuth staffAuth = staffAuthRepo.searchByEmail(email);
        if (staffAuth == null) {
            System.out.println("ERROR: Staff not found!");
            //todo: notify that staff 404
        }
        String jwt = jwtService.generateStaffJWToken(staffAuth.getStaffAuthId());

        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", jwt)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        response.sendRedirect("http://localhost:3000/oauth2/success?token=" + jwt);
    }
}
