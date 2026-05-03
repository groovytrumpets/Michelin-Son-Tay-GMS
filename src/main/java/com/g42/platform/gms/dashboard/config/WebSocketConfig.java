package com.g42.platform.gms.dashboard.config;
import com.g42.platform.gms.auth.entity.StaffAuth;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.service.JWTService;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Autowired
    private JWTService jwtService;
    @Autowired
    private StaffAuthRepo staffAuthRepo;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic","/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications").setAllowedOrigins("http://localhost:5173",
                "https://sontaygarage.vn",
                "https://api.sontaygarage.vn","http://127.0.0.1:5500").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor!=null&& StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization!=null && !authorization.isEmpty()) {
                        String bearerToken = authorization.get(0);
                        if (bearerToken.startsWith("Bearer ")) {
                            String token = bearerToken.substring(7);

                            // Giải mã token (Dùng logic hiện có của dự án)
                            if (jwtService.validateToken(token)) {
                                // QUAN TRỌNG: Lấy staffId ra (ví dụ: "1", "2")
                                StaffAuth staffAuth = staffAuthRepo.searchByStaffAuthId(jwtService.extractAuthId(token));
                                String staffId = String.valueOf(staffAuth.getStaffProfile().getStaffId());


                                // Gán Principal có name CHÍNH LÀ staffId
                                Principal userPrincipal = () -> staffId;

                                // Nếu bạn muốn dùng Spring Security Authentication đầy đủ:
                                // Principal userPrincipal = new UsernamePasswordAuthenticationToken(staffId, null, authorities);

                                accessor.setUser(userPrincipal);
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
