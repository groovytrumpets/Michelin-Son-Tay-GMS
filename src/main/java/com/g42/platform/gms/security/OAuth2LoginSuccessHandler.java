package com.g42.platform.gms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.dto.AuthResponse;
import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.service.JWTService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            ResponseEntity<ApiResponse<AuthResponse>> responseResponseEntity = ResponseEntity.ok(ApiResponses.error(AuthErrorCode.USER_NOT_FOUND.name(),"Tài khoản chưa có trong hệ thống"));
            response.setContentType("application/json");
            response.getWriter().write(
                    new ObjectMapper().writeValueAsString(responseResponseEntity)
            );
            return;
        }
        if (staffAuth.getStatus().equals("LOCKED")){
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account locked");
            return;
        }

        staffAuth.setGoogle_id(googleId);
        System.err.println("GOOGLE ID: "+googleId);
        staffAuth.setAuthProvider("GOOGLE");
        staffAuthRepo.save(staffAuth);
        String jwt = jwtService.generateStaffJWToken(staffAuth.getStaffAuthId());
//        response.sendRedirect("http://localhost:3000/oauth2/success?token=" + jwt);
        AuthResponse authResponse = new AuthResponse("GOOGLE OAUTH2 SUCCESS","STAFF",jwt);

        ResponseEntity<ApiResponse<AuthResponse>> responseResponseEntity = ResponseEntity.ok(ApiResponses.success(authResponse));
        response.setContentType("application/json");
        response.getWriter().write(
                new ObjectMapper().writeValueAsString(responseResponseEntity)
        );
    }
}
