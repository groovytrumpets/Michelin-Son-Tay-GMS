package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.dto.LoginRequest;
import com.g42.platform.gms.auth.dto.StaffAuthResponse;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;

import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.repository.StaffRoleRepository;
import com.g42.platform.gms.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.RedirectStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StaffAuthServiceTest {
    @Mock
    private StaffAuthRepo staffAuthRepo;
    @Mock
    private StaffProfileRepo staffProfileRepo;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JWTService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private StaffAuthService staffAuthService;
    @Mock private StaffRoleRepository staffRoleRepo;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;
    @Mock private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2LoginSuccessHandler handler;
    @Test
    @DisplayName("UTCID08")
    void loginSuccessReturnToken(){
        String email = "abc@gmail.com";
        String password = "111111";

        // 1. CHUẨN BỊ MOCK DỮ LIỆU
        StaffAuth mockStaffAuth = new StaffAuth();
        mockStaffAuth.setStaffAuthId(1); // Nhớ set ID vì lúc sau getStaffProfileByStaffauth_StaffAuthId có dùng tới
        mockStaffAuth.setEmail(email);
        mockStaffAuth.setPasswordHash("$2a$12$fJFRZoQwxm1zChjTFtBaN.gu3dzaiph8h.PIEhEogouF0wmYZMu0q");

        StaffProfile mockStaffProfile = new StaffProfile();
        mockStaffProfile.setStaffId(1);
        mockStaffProfile.setFullName("NNK");

        // 2. CẤU HÌNH CÁC REPOSITORY VÀ ENCODER
        // Lưu ý: Mình bỏ mock staffAuthRepo.getStaffAuthByEmail và passwordEncoder.matches
        // vì AuthenticationManager của Spring (code thật) sẽ tự động làm việc đó ẩn bên dưới,
        // trong hàm test service này bạn không trực tiếp gọi chúng nên mock là thừa.

        // 3. CẤU HÌNH MOCK CHO QUÁ TRÌNH XÁC THỰC
        Authentication mockAuthentication = mock(Authentication.class);
        StaffPrincipal mockStaffPrincipal = mock(StaffPrincipal.class);

        // Nối dây chuyền
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mockAuthentication);
        when(mockAuthentication.getPrincipal()).thenReturn(mockStaffPrincipal);
        when(mockStaffPrincipal.getStaffAuth()).thenReturn(mockStaffAuth);

        // BẮT BUỘC THÊM 2 DÒNG NÀY ĐỂ VƯỢT QUA LỆNH IF
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockStaffPrincipal.isAccountNonLocked()).thenReturn(true);
        when(mockStaffPrincipal.getAuthId()).thenReturn(1); // Cho jwtService dùng

        // 4. CẤU HÌNH MOCK CHO QUÁ TRÌNH LẤY THÔNG TIN
        when(jwtService.generateStaffJWToken(any())).thenReturn("mock_token_123");
        // Chú ý: Bạn cần khai báo thêm @Mock private StaffRoleRepo staffRoleRepo; ở trên đầu file nhé
        when(staffProfileRepo.getStaffProfileByStaffauth_StaffAuthId(mockStaffAuth.getStaffAuthId())).thenReturn(mockStaffProfile);
        // Tạm thời mock trả về list rỗng cho nhanh qua bài, nếu cần test chi tiết role thì add vào
        when(staffRoleRepo.getStaffRoleByStaff_StaffId(mockStaffProfile.getStaffId())).thenReturn(List.of());

        // 5. TẠO REQUEST (ACT)
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(email);
        loginRequest.setPin(password); // Đưa mật khẩu gốc vào, không phải mã băm!

        // 6. THỰC THI (ACT)
        StaffAuthResponse staffAuthResponse = staffAuthService.verifyStaffAuth(loginRequest);

        // 7. KIỂM TRA (ASSERT)
        assertNotNull(staffAuthResponse);
        assertEquals("LOGIN_SUCCESS", staffAuthResponse.getMessage()); // Hoặc tùy field của bạn
        assertEquals("mock_token_123", staffAuthResponse.getToken());
    }
    @Test
    @DisplayName("UTCID01")
    void loginFailWithNull(){
        String email = null;
        String password = null;
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(email);
        loginRequest.setPin(password);

        AuthException exception = assertThrows(AuthException.class, () -> {
            staffAuthService.verifyStaffAuth(loginRequest);
        });


        assertEquals("Số điện thoại hoặc email là bắt buộc", exception.getMessage());
    }
    @Test
    @DisplayName("UTCID02")
    void loginFailWithPassNull(){
        String email = "abc@";
        String password = null;
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(email);
        loginRequest.setPin(password);

        AuthException exception = assertThrows(AuthException.class, () -> {
            staffAuthService.verifyStaffAuth(loginRequest);
        });


        assertEquals("Số điện thoại hoặc email là bắt buộc", exception.getMessage());
    }
    @Test
    @DisplayName("UTCID03")
    void loginFailWithWrongEmailPassword() {

        String email = "abc@";
        String password = "123456678";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(email);
        loginRequest.setPin(password);

        AuthException exception = assertThrows(AuthException.class, () -> {
            staffAuthService.verifyStaffAuth(loginRequest);
        });
        assertEquals("Số điện thoại hoặc email không hợp lệ", exception.getMessage());
    }
    @Test
    @DisplayName("UTCID05")
    void loginFailWithWrongPassword() {

        String email = "abc@gmail.com";
        String password = "123   456678";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(email);
        loginRequest.setPin(password);

        StaffAuth mockStaffAuth = new StaffAuth();
        StaffPrincipal mockStaffPrincipal = mock(StaffPrincipal.class);
        Authentication mockAuthentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mockAuthentication);
        when(mockAuthentication.getPrincipal()).thenReturn(mockStaffPrincipal);
        when(mockStaffPrincipal.getStaffAuth()).thenReturn(mockStaffAuth);
        when(mockAuthentication.isAuthenticated()).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> {
            staffAuthService.verifyStaffAuth(loginRequest);
        });
        assertEquals("Sai thông tin đăng nhập, tài khoản có thể bị khóa sau 10 lần thử", exception.getMessage());
    }
    @Test
    @DisplayName("UTCID06")
    void loginFailWithWrongEmailFormat() {

        String email = "abc@email.com    ";
        String password = "123456678";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(email);
        loginRequest.setPin(password);

        AuthException exception = assertThrows(AuthException.class, () -> {
            staffAuthService.verifyStaffAuth(loginRequest);
        });
        assertEquals("Số điện thoại hoặc email không hợp lệ", exception.getMessage());
    }
    @Test
    @DisplayName("UTCID07")
    void loginInactiveAccount() {
        String email = "abc@gmail.com";
        String password = "111111";


        StaffAuth mockStaffAuth = new StaffAuth();
        mockStaffAuth.setStaffAuthId(1);
        mockStaffAuth.setEmail(email);
        mockStaffAuth.setPasswordHash("$2a$12$fJFRZoQwxm1zChjTFtBaN.gu3dzaiph8h.PIEhEogouF0wmYZMu0q");

        StaffProfile mockStaffProfile = new StaffProfile();
        mockStaffProfile.setStaffId(1);
        mockStaffProfile.setFullName("NNK");

        Authentication mockAuthentication = mock(Authentication.class);
        StaffPrincipal mockStaffPrincipal = mock(StaffPrincipal.class);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mockAuthentication);
        when(mockAuthentication.getPrincipal()).thenReturn(mockStaffPrincipal);
        when(mockStaffPrincipal.getStaffAuth()).thenReturn(mockStaffAuth);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockStaffPrincipal.isAccountNonLocked()).thenReturn(false);


        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(email);
        loginRequest.setPin(password);


        AuthException exception = assertThrows(AuthException.class, () -> {
            staffAuthService.verifyStaffAuth(loginRequest);
        });

        assertEquals("Tài khoản đã bị khóa hoặc chưa kích hoạt", exception.getMessage());
    }
    @Test
    @DisplayName("UTCID10")
    void loginUsingPhoneSuccessReturnToken(){
        String phone = "0999999999";
        String password = "111111";

        StaffAuth mockStaffAuth = new StaffAuth();
        mockStaffAuth.setStaffAuthId(1);
        mockStaffAuth.setEmail(phone);
        mockStaffAuth.setPasswordHash("$2a$12$fJFRZoQwxm1zChjTFtBaN.gu3dzaiph8h.PIEhEogouF0wmYZMu0q");

        StaffProfile mockStaffProfile = new StaffProfile();
        mockStaffProfile.setStaffId(1);
        mockStaffProfile.setFullName("NNK");

        Authentication mockAuthentication = mock(Authentication.class);
        StaffPrincipal mockStaffPrincipal = mock(StaffPrincipal.class);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mockAuthentication);
        when(mockAuthentication.getPrincipal()).thenReturn(mockStaffPrincipal);
        when(mockStaffPrincipal.getStaffAuth()).thenReturn(mockStaffAuth);

        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockStaffPrincipal.isAccountNonLocked()).thenReturn(true);
        when(mockStaffPrincipal.getAuthId()).thenReturn(1);

        when(jwtService.generateStaffJWToken(any())).thenReturn("mock_token_123");

        when(staffProfileRepo.getStaffProfileByStaffauth_StaffAuthId(mockStaffAuth.getStaffAuthId())).thenReturn(mockStaffProfile);

        when(staffRoleRepo.getStaffRoleByStaff_StaffId(mockStaffProfile.getStaffId())).thenReturn(List.of());


        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone(phone);
        loginRequest.setPin(password);

        StaffAuthResponse staffAuthResponse = staffAuthService.verifyStaffAuth(loginRequest);

        assertNotNull(staffAuthResponse);
        assertEquals("LOGIN_SUCCESS", staffAuthResponse.getMessage());
        assertEquals("mock_token_123", staffAuthResponse.getToken());
    }
    @Test
    @DisplayName("UTCID13")
    void onAuthSuccess_WhenEmailNotFound_ShouldRedirectToLoginWithError() throws Exception {
        String email = "abc@email";

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("sub")).thenReturn("google_id_123");

        when(staffAuthRepo.searchByEmail(email)).thenReturn(null);

        handler.setRedirectStrategy(redirectStrategy);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:5173/login?error=USER_NOT_FOUND");

        verify(staffAuthRepo, never()).save(any());
    }
    @Test
    @DisplayName("UTCID11")
    void onAuthSuccess_WhenValidEmail_ShouldRedirectToDashboardWithToken() throws Exception {
        String validEmail = "abc@email.com";
        StaffAuth mockAuth = new StaffAuth();
        mockAuth.setStaffAuthId(1);
        mockAuth.setStatus("ACTIVE");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(validEmail);
        when(oAuth2User.getAttribute("sub")).thenReturn("google_id_456");

        when(staffAuthRepo.searchByEmail(validEmail)).thenReturn(mockAuth);
        when(jwtService.generateStaffJWToken(1)).thenReturn("super_secret_token_123");

        handler.setRedirectStrategy(redirectStrategy);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(staffAuthRepo).save(mockAuth);

        verify(response).sendRedirect("http://localhost:5173/dashboard?token=super_secret_token_123");
    }
}
