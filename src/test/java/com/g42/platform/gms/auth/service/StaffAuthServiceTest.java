package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.LoginRequest;
import com.g42.platform.gms.auth.dto.StaffAuthResponse;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;

import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.repository.StaffRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Test
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
}
