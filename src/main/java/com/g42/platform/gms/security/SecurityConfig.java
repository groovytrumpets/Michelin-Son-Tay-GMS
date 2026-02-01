package com.g42.platform.gms.security;

import com.g42.platform.gms.auth.filter.StaffJwtFilter;
import com.g42.platform.gms.auth.service.StaffAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    StaffJwtFilter staffJwtFilter;
    @Autowired
    AuthenticationSuccessHandler OAuth2LoginSuccessHandler;

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //có bị sai ko?
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil);

        http
                .csrf(csrf -> csrf.disable())
//                .sessionManagement(sm ->
//                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
                .authorizeHttpRequests(auth -> auth
                        // Cấu hình danh sách API cho phép truy cập tự do (Không cần Token)
                        .requestMatchers(
                                "/api/auth/customer/check-status",
                                "/api/auth/customer/request-otp",
                                "/api/auth/customer/verify-otp",
                                "/api/auth/customer/setup-pin",
                                "/api/auth/customer/login",
                                "/api/auth/**",      // Giữ cái này làm fallback
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()
                        // Tất cả các API khác bắt buộc phải có Token
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(OAuth2LoginSuccessHandler))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
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