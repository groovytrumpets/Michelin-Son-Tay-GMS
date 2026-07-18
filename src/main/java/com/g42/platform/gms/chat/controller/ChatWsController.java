package com.g42.platform.gms.chat.controller;

import com.g42.platform.gms.chat.dto.MarkReadRequest;
import com.g42.platform.gms.chat.dto.SendMessageRequest;
import com.g42.platform.gms.chat.dto.TypingRequest;
import com.g42.platform.gms.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP handler cho /ws-chat. Principal ở đây chỉ có getName() = staffId (bare Principal
 * gán bởi ChannelInterceptor trong dashboard/config/WebSocketConfig.java khi CONNECT),
 * KHÔNG dùng được @AuthenticationPrincipal StaffPrincipal như REST controller.
 */
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageRequest request, Principal principal) {
        Integer senderId = resolveStaffId(principal);
        if (senderId == null || request.getConversationId() == null) return;
        chatService.sendMessage(request.getConversationId(), senderId, request);
    }

    @MessageMapping("/chat.read")
    public void read(@Payload MarkReadRequest request, Principal principal) {
        Integer staffId = resolveStaffId(principal);
        if (staffId == null || request.getConversationId() == null) return;
        chatService.markRead(staffId, request.getConversationId(), request.getUpToMessageId());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequest request, Principal principal) {
        Integer staffId = resolveStaffId(principal);
        if (staffId == null || request.getConversationId() == null) return;
        chatService.broadcastTyping(request.getConversationId(), staffId, request.isTyping());
    }

    private Integer resolveStaffId(Principal principal) {
        if (principal == null) return null;
        try {
            return Integer.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
