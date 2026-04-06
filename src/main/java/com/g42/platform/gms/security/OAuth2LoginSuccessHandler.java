package com.g42.platform.gms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.dto.AuthResponse;
import com.g42.platform.gms.auth.dto.StaffAuthResponse;
import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffRoleRepository;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@AllArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTService jwtService;
    private final StaffAuthRepo staffAuthRepo;
    private final StaffRoleRepository staffRoleRepository;

        @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttribute("email");
        String googleId = (String) oAuth2User.getAttribute("sub");

        StaffAuth staffAuth = staffAuthRepo.searchByEmail(email);
        if (staffAuth == null) {
            System.err.println("ERROR: Unauthorized Google Login attempt - " + email);
            getRedirectStrategy().sendRedirect(request, response, frontendLoginUrl + "?error=USER_NOT_FOUND");
            return;
        }
        if (staffAuth.getStatus().equals("LOCKED")){
            getRedirectStrategy().sendRedirect(request, response, frontendLoginUrl + "?error=ACCOUNT_LOCKED");
            return;
        }

        staffAuth.setGoogle_id(googleId);
        System.err.println("GOOGLE ID: " + googleId);
        staffAuth.setAuthProvider("GOOGLE");
        staffAuthRepo.save(staffAuth);
        StaffProfile staffProfile = staffAuth.getStaffProfile();
        String jwt = jwtService.generateStaffJWToken(staffAuth.getStaffAuthId());

        StaffProfile staffProfile = staffAuth.getStaffProfile();
            List<String> roles = staffRoleRepository
                    .getStaffRoleByStaff_StaffId(staffProfile.getStaffId())
                    .stream()
                    .map(staffRole -> staffRole.getRole().getRoleCode())
                    .toList();
            StaffAuthResponse authResponse = new StaffAuthResponse(staffProfile.getStaffId(),staffProfile.getFullName(),staffProfile.getAvatar(),"",roles,jwt);
            ObjectMapper mapper = new ObjectMapper();
            String jsonInfo = mapper.writeValueAsString(authResponse);

            // 2. Mã hóa chuỗi JSON thành Base64 để nó thành một dải chữ và số an toàn
            String base64Info = Base64.getUrlEncoder().encodeToString(jsonInfo.getBytes(StandardCharsets.UTF_8));

            // 3. Đá về Frontend với chuẩn URL: ?token=...&info=...
            response.sendRedirect(frontendHomeUrl + "?token=" + jwt + "&info=" + base64Info);
    }
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//        String email = (String) oAuth2User.getAttribute("email");
//        String googleId = (String) oAuth2User.getAttribute("sub");
//
//        StaffAuth staffAuth = staffAuthRepo.searchByEmail(email);
//        if (staffAuth == null) {
//            System.out.println("ERROR: Staff not found!");
//            //todo: notify that staff 404
//            ResponseEntity<ApiResponse<AuthResponse>> responseResponseEntity = ResponseEntity.ok(ApiResponses.error(AuthErrorCode.USER_NOT_FOUND.name(), "Tài khoản chưa có trong hệ thống"));
//            response.setContentType("application/json;charset=UTF-8");
//            response.getWriter().write(
//                    new ObjectMapper().writeValueAsString(responseResponseEntity)
//            );
//            return;
//        }
//        if (staffAuth.getStatus().equals("LOCKED")) {
//            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account locked");
//            return;
//        }
//
//        staffAuth.setGoogle_id(googleId);
//        System.err.println("GOOGLE ID: " + googleId);
//        staffAuth.setAuthProvider("GOOGLE");
//        staffAuthRepo.save(staffAuth);
//        StaffProfile staffProfile = staffAuth.getStaffProfile();
//        String jwt = jwtService.generateStaffJWToken(staffAuth.getStaffAuthId());
////        response.sendRedirect("http://localhost:3000/oauth2/success?token=" + jwt);
//        AuthResponse authResponse = new AuthResponse("GOOGLE OAUTH2 SUCCESS", "STAFF", jwt);
//        List<String> roles = staffRoleRepository
//                .getStaffRoleByStaff_StaffId(staffProfile.getStaffId())
//                .stream()
//                .map(staffRole -> staffRole.getRole().getRoleCode())
//                .toList();
//        StaffAuthResponse authResponse1 = new StaffAuthResponse(staffProfile.getStaffId(),staffProfile.getFullName(),staffProfile.getAvatar(),"",roles,jwt);
//
//        ResponseEntity<ApiResponse<StaffAuthResponse>> responseResponseEntity = ResponseEntity.ok(ApiResponses.success(authResponse1));
//        response.setContentType("application/json");
//        response.getWriter().write(
//                new ObjectMapper().writeValueAsString(responseResponseEntity)
//        );
//    }
}
