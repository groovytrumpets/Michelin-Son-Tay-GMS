package com.g42.platform.gms.chat.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.chat.dto.*;
import com.g42.platform.gms.chat.service.ChatService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<List<ContactDto>>> contacts(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponses.success(chatService.listContacts(principal.getStaffId(), search)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> conversations(
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(chatService.listConversations(principal.getStaffId())));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody ConversationCreateRequest request) {
        return ResponseEntity.ok(ApiResponses.success(chatService.createConversation(principal.getStaffId(), request)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessagePageDto>> messages(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer conversationId,
            @RequestParam(required = false) Integer before,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponses.success(
                chatService.listMessages(principal.getStaffId(), conversationId, before, limit)));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                chatService.sendMessage(conversationId, principal.getStaffId(), request)));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Boolean>> markRead(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer conversationId,
            @RequestBody MarkReadRequest request) {
        chatService.markRead(principal.getStaffId(), conversationId, request.getUpToMessageId());
        return ResponseEntity.ok(ApiResponses.success(true));
    }
}
