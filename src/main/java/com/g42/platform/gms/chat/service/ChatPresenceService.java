package com.g42.platform.gms.chat.service;

import com.g42.platform.gms.chat.dto.PresenceEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Theo dõi online/offline nhân viên qua session STOMP (/ws-chat), hoàn toàn in-memory,
 * không persist DB. Mỗi staffId có thể có nhiều session (nhiều tab/thiết bị) —
 * chỉ báo offline khi session cuối cùng đóng.
 */
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final Map<String, Set<String>> sessionsByStaffId = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (user == null || sessionId == null) return;

        Set<String> sessions = sessionsByStaffId.computeIfAbsent(user.getName(), k -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();
        sessions.add(sessionId);

        if (wasOffline) {
            broadcastPresence(user.getName(), true);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (user == null || sessionId == null) return;

        Set<String> sessions = sessionsByStaffId.get(user.getName());
        if (sessions == null) return;
        sessions.remove(sessionId);

        if (sessions.isEmpty()) {
            sessionsByStaffId.remove(user.getName());
            broadcastPresence(user.getName(), false);
        }
    }

    private void broadcastPresence(String staffIdStr, boolean online) {
        try {
            simpMessagingTemplate.convertAndSend("/topic/chat-presence",
                    new PresenceEventDto(Integer.valueOf(staffIdStr), online));
        } catch (NumberFormatException ignored) {
            // Principal name không phải staffId hợp lệ, bỏ qua.
        }
    }

    public boolean isOnline(Integer staffId) {
        if (staffId == null) return false;
        Set<String> sessions = sessionsByStaffId.get(staffId.toString());
        return sessions != null && !sessions.isEmpty();
    }
}
