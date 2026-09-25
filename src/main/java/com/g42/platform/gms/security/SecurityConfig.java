package com.g42.platform.gms.security;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.security.filter.JwtAuthenticationFilter;
import com.g42.platform.gms.security.filter.StaffJwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;  // ← THÊM
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableMethodSecurity  
public class SecurityConfig {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    StaffJwtFilter staffJwtFilter;
    @Autowired
    AuthenticationSuccessHandler OAuth2LoginSuccessHandler;
    @Autowired
    JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Nhóm đường chỉ dành cho nhân viên. Đã rà FE (2026-09-24): trang khách chỉ gọi
     * /api/customer/**, /api/booking/customer/**, /api/public/**, /home/** và
     * /api/warehouse/search/** — không đường nào dưới đây. Thêm API cho khách thì
     * đặt dưới /api/customer/ chứ đừng nhét vào các nhóm này.
     */
    private static final String[] STAFF_ONLY_PATHS = {
            "/api/admin/**",
            "/api/manager/**",
            "/api/staff/**",
            "/api/staff-profile/**",
            "/api/staff-notification/**",
            "/api/staff-workload/**",
            "/api/receptionist/**",
            "/api/service-ticket/**",
            "/api/safety-inspections/**",
            "/api/work-history/**",
            "/api/warehouse/**",
            "/api/payment/**",
            "/api/kpi/**",
            "/api/dashboard/**",
            "/api/reports/**",
            "/api/booking/manage/**",
            "/api/booking/staff/**",
            "/api/promotion/**",
            "/api/service/**",
            "/api/feedback/**",
            "/api/ai-assistant/**",
            "/api/chat/**",
            "/api/push/**",
            "/api/bug-reports/**",
            "/api/v1/docs/**",
            "/api/vehicles/**",
            // /zalo/otp, /zalo/booking-confirmed... là endpoint thử gửi tin Zalo.
            "/zalo/**",
    };

    /** Người gọi phải đăng nhập bằng token nhân viên (không phải token khách). */
    private static final AuthorizationManager<RequestAuthorizationContext> STAFF_ONLY =
            (authentication, context) -> new AuthorizationDecision(
                    authentication.get() != null && authentication.get().getPrincipal() instanceof StaffPrincipal);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173","http://staff.localhost:5173",
                            "https://sontaygarage.vn","https://api.sontaygarage.vn","https://staff.sontaygarage.vn","http://127.0.0.1:5500",
                            "https://demo.sontaygarage.vn","https://demoapi.sontaygarage.vn","https://staff.demo.sontaygarage.vn"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    // X-Branch-Id: xưởng mà máy đang dùng đã chọn (xem BranchDirectory)
                    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Branch-Id"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/booking/guest/**",
                                "/api/booking/slots/**",
                                // Giờ mở cửa/giới hạn đặt lịch — form đặt lịch của khách vãng lai cần.
                                "/api/booking/schedule-config",
                                "/api/catalog/**",
                                // Chỉ danh mục hãng/dòng xe là công khai (form đặt lịch). Trước đây
                                // mở cả /api/vehicles/** nên ai cũng xem được xe của khách bất kỳ
                                // qua /api/vehicles/customer/{id}.
                                "/api/vehicles/brands/**",
                                // /api/receptionist/** (check-in) từng nằm ở đây: người chưa đăng
                                // nhập tra được khách theo SĐT và tạo được phiếu. Đã bỏ.
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/zalo/callback",
                                "/zalo/login",
                                // Google tự redirect trình duyệt về đây sau khi cấp quyền GA4/Search
                                // Console — không kèm Authorization header nên phải công khai; an toàn
                                // nhờ tham số state đối chiếu ở GoogleOAuthService#handleCallback.
                                "/api/admin/analytics/google/callback",
                                // Trang công khai /car-parts-lookup. Trước ghi sai "item-categoy"
                                // nên dòng này chưa từng có tác dụng.
                                "/api/warehouse/item-category/all",
                                "/api/warehouse/search/catalog-items-detail",
                                "/home/**",
                                "/api/webhook/**",
                                "/error",
                                "/ws-notifications/**",
                                "/ws-chat/**",
                                "/api/public/sliders/**",
                                "/api/public/landing-page-catalog",
                                // Tin tức: trang bài viết phải đọc được khi chưa đăng nhập,
                                // và /seo/** là nơi bot Facebook/Zalo/Google lấy thẻ meta.
                                "/api/public/posts/**",
                                "/api/public/post-categories/**",
                                "/api/public/post-tags/**",
                                // Bài viết phụ tùng: nội dung của trang chi tiết dịch vụ/phụ tùng
                                // (/services/<slug>, /parts/<slug>) và của /danh-muc. Thiếu dòng này
                                // thì khách chưa đăng nhập bấm "Xem chi tiết" ở trang chủ sẽ ăn 401.
                                "/api/public/item-posts/**",
                                "/api/public/item-post-categories/**",
                                "/api/public/item-post-tags/**",
                                // Thanh menu phải dựng được trước khi khách đăng nhập.
                                "/api/public/nav-menu/**",
                                // Bố cục thanh đầu trang (logo, ô tìm kiếm, nút) — cùng lý do.
                                "/api/public/site-header",
                                // Tuyển dụng: ứng viên xem tin và nộp hồ sơ mà không có tài khoản.
                                "/api/public/recruitment/**",
                                // Danh sách xưởng cho form khách đặt lịch online.
                                "/api/public/branches",
                                "/seo/**"
                        ).permitAll()
                        // Trang khách (tra phụ tùng, bài viết) dùng chung API tìm kiếm của kho,
                        // nên nhánh này phải đứng trước khối "chỉ nhân viên" bên dưới.
                        .requestMatchers("/api/warehouse/search/**").authenticated()
                        // Token của khách cũng "authenticated", nên các API nhân viên không có
                        // @PreAuthorize từng mở cho khách đăng nhập web gọi thẳng (tạo khuyến
                        // mãi, đổi trạng thái phiếu, xem danh bạ...). Chặn ở tầng URL cho chắc,
                        // @PreAuthorize từng endpoint vẫn là lớp kiểm tra mã quyền riêng.
                        .requestMatchers(STAFF_ONLY_PATHS).access(STAFF_ONLY)
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(OAuth2LoginSuccessHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String path = request.getRequestURI();
                            String acceptHeader = request.getHeader("Accept");

                            boolean isApiRequest = path.startsWith("/api/")
                                    || (acceptHeader != null && acceptHeader.contains("application/json"));

                            if (isApiRequest) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json; charset=UTF-8");
                                response.getWriter().write("""
                            {
                              "success": false,
                              "code": "UNAUTHORIZED",
                              "message": "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."
                            }
                            """);
                            } else {
                                response.sendRedirect("/oauth2/authorization/google");
                            }
                        })
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(staffJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
