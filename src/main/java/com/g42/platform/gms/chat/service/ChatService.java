package com.g42.platform.gms.chat.service;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.chat.dto.*;
import com.g42.platform.gms.chat.entity.ChatMessageJpa;
import com.g42.platform.gms.chat.entity.ConversationJpa;
import com.g42.platform.gms.chat.entity.ConversationParticipantJpa;
import com.g42.platform.gms.chat.entity.MessageAttachmentJpa;
import com.g42.platform.gms.chat.exception.ChatErrorCode;
import com.g42.platform.gms.chat.exception.ChatException;
import com.g42.platform.gms.chat.repository.ChatMessageJpaRepo;
import com.g42.platform.gms.chat.repository.ConversationJpaRepo;
import com.g42.platform.gms.chat.repository.ConversationParticipantJpaRepo;
import com.g42.platform.gms.chat.repository.MessageAttachmentJpaRepo;
import com.g42.platform.gms.push.application.service.WebPushDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationJpaRepo conversationJpaRepo;
    private final ConversationParticipantJpaRepo participantJpaRepo;
    private final ChatMessageJpaRepo messageJpaRepo;
    private final MessageAttachmentJpaRepo attachmentJpaRepo;
    private final StaffProfileRepo staffProfileRepo;
    private final ChatPresenceService presenceService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final WebPushDispatchService webPushDispatchService;

    // ===================== Contacts =====================

    public List<ContactDto> listContacts(Integer currentStaffId, String search) {
        List<StaffProfile> profiles = StringUtils.hasText(search)
                ? staffProfileRepo.findByFullNameContainingIgnoreCase(search.trim())
                : staffProfileRepo.findAll();

        return profiles.stream()
                .filter(p -> p.getStaffId() != null && !p.getStaffId().equals(currentStaffId))
                .filter(p -> p.getStaffauth() == null || !"LOCKED".equalsIgnoreCase(p.getStaffauth().getStatus()))
                .map(this::toContactDto)
                // Người đang online ưu tiên lên đầu, trong mỗi nhóm (online/offline) vẫn xếp A-Z theo tên.
                .sorted(Comparator
                        .comparing(ContactDto::isOnline).reversed()
                        .thenComparing(ContactDto::getFullName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private ContactDto toContactDto(StaffProfile p) {
        List<String> roles = p.getStaffRoles() == null ? List.of()
                : p.getStaffRoles().stream()
                        .filter(sr -> sr.getRole() != null)
                        .map(sr -> sr.getRole().getRoleCode())
                        .toList();
        return new ContactDto(p.getStaffId(), p.getFullName(), p.getAvatar(), roles,
                presenceService.isOnline(p.getStaffId()), null);
    }

    // ===================== Conversations =====================

    public List<ConversationDto> listConversations(Integer staffId) {
        List<ConversationParticipantJpa> mine = participantJpaRepo.findByStaffId(staffId);
        return mine.stream()
                .map(p -> buildConversationDto(p.getConversationId(), staffId))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ConversationDto::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private ConversationDto buildConversationDto(Integer conversationId, Integer staffId) {
        ConversationJpa conv = conversationJpaRepo.findById(conversationId).orElse(null);
        if (conv == null) return null;

        List<ConversationParticipantJpa> participants = participantJpaRepo.findByConversationId(conversationId);
        ConversationParticipantJpa myParticipant = participants.stream()
                .filter(p -> p.getStaffId().equals(staffId)).findFirst().orElse(null);

        List<ContactDto> others = participants.stream()
                .filter(p -> !p.getStaffId().equals(staffId))
                .map(p -> staffProfileRepo.findById(p.getStaffId()).orElse(null))
                .filter(Objects::nonNull)
                .map(this::toContactDto)
                .toList();

        ChatMessageJpa lastMsgJpa = messageJpaRepo.findTopByConversationIdOrderByMessageIdDesc(conversationId).orElse(null);
        MessageDto lastMessage = lastMsgJpa == null ? null : toMessageDto(lastMsgJpa, List.of(), participants);

        Integer lastRead = myParticipant != null ? myParticipant.getLastReadMessageId() : null;
        long unread = lastRead != null
                ? messageJpaRepo.countByConversationIdAndSenderIdNotAndMessageIdGreaterThan(conversationId, staffId, lastRead)
                : messageJpaRepo.countByConversationIdAndSenderIdNot(conversationId, staffId);

        boolean isDirect = "DIRECT".equalsIgnoreCase(conv.getType());
        String title = conv.getTitle();
        String avatarUrl = null;
        if (isDirect && !others.isEmpty()) {
            title = others.get(0).getFullName();
            avatarUrl = others.get(0).getAvatarUrl();
        }

        ConversationDto dto = new ConversationDto();
        dto.setConversationId(conv.getConversationId());
        dto.setType(isDirect ? "direct" : "group");
        dto.setTitle(title);
        dto.setAvatarUrl(avatarUrl);
        dto.setParticipants(others);
        dto.setLastMessage(lastMessage);
        dto.setUnreadCount(unread);
        dto.setUpdatedAt(conv.getUpdatedAt());
        return dto;
    }

    @Transactional
    public ConversationDto createConversation(Integer staffId, ConversationCreateRequest req) {
        LinkedHashSet<Integer> participantIds = new LinkedHashSet<>(req.getParticipantIds());
        participantIds.add(staffId);
        if (participantIds.size() < 2) {
            throw new ChatException(ChatErrorCode.INVALID_PARTICIPANTS);
        }

        boolean isDirect = !"group".equalsIgnoreCase(req.getType());

        if (isDirect && participantIds.size() == 2) {
            Integer otherId = participantIds.stream().filter(id -> !id.equals(staffId)).findFirst().orElse(null);
            Integer existingConvId = findExistingDirectConversationId(staffId, otherId);
            if (existingConvId != null) {
                return buildConversationDto(existingConvId, staffId);
            }
        }

        List<StaffProfile> profiles = staffProfileRepo.findAllById(participantIds);
        if (profiles.size() != participantIds.size()) {
            throw new ChatException(ChatErrorCode.STAFF_NOT_FOUND);
        }

        ConversationJpa conv = new ConversationJpa();
        conv.setType(isDirect ? "DIRECT" : "GROUP");
        conv.setTitle(isDirect ? null : req.getTitle());
        conv.setCreatedBy(staffId);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conv = conversationJpaRepo.save(conv);

        for (Integer pid : participantIds) {
            ConversationParticipantJpa p = new ConversationParticipantJpa();
            p.setConversationId(conv.getConversationId());
            p.setStaffId(pid);
            p.setJoinedAt(LocalDateTime.now());
            participantJpaRepo.save(p);
        }

        return buildConversationDto(conv.getConversationId(), staffId);
    }

    private Integer findExistingDirectConversationId(Integer staffId, Integer otherId) {
        if (otherId == null) return null;
        Set<Integer> myConvIds = participantJpaRepo.findByStaffId(staffId).stream()
                .map(ConversationParticipantJpa::getConversationId).collect(java.util.stream.Collectors.toSet());
        Set<Integer> otherConvIds = participantJpaRepo.findByStaffId(otherId).stream()
                .map(ConversationParticipantJpa::getConversationId).collect(java.util.stream.Collectors.toSet());
        myConvIds.retainAll(otherConvIds);

        for (Integer convId : myConvIds) {
            ConversationJpa conv = conversationJpaRepo.findById(convId).orElse(null);
            if (conv != null && "DIRECT".equalsIgnoreCase(conv.getType())
                    && participantJpaRepo.findByConversationId(convId).size() == 2) {
                return convId;
            }
        }
        return null;
    }

    // ===================== Messages =====================

    public MessagePageDto listMessages(Integer staffId, Integer conversationId, Integer before, Integer limit) {
        ensureParticipant(conversationId, staffId);
        int pageSize = (limit == null || limit <= 0) ? 30 : limit;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<ChatMessageJpa> rows = messageJpaRepo.findPage(conversationId, before, pageable);

        boolean hasMore = rows.size() > pageSize;
        List<ChatMessageJpa> page = hasMore ? rows.subList(0, pageSize) : rows;
        List<ConversationParticipantJpa> participants = participantJpaRepo.findByConversationId(conversationId);

        List<MessageDto> messages = page.stream()
                .map(m -> toMessageDto(m, attachmentsOf(m.getMessageId()), participants))
                .toList();

        Integer nextCursor = (hasMore && !page.isEmpty()) ? page.get(page.size() - 1).getMessageId() : null;
        return new MessagePageDto(messages, hasMore, nextCursor);
    }

    @Transactional
    public MessageDto sendMessage(Integer conversationId, Integer senderId, SendMessageRequest req) {
        List<ConversationParticipantJpa> participants = participantJpaRepo.findByConversationId(conversationId);
        if (participants.isEmpty()) {
            throw new ChatException(ChatErrorCode.CONVERSATION_NOT_FOUND);
        }
        ensureParticipant(participants, senderId);

        Optional<ChatMessageJpa> existing = messageJpaRepo.findByConversationIdAndClientMsgId(conversationId, req.getClientMsgId());

        ChatMessageJpa saved;
        List<AttachmentDto> attachmentDtos;

        if (existing.isPresent()) {
            saved = existing.get();
            attachmentDtos = attachmentsOf(saved.getMessageId());
        } else {
            ChatMessageJpa msg = new ChatMessageJpa();
            msg.setConversationId(conversationId);
            msg.setSenderId(senderId);
            msg.setClientMsgId(req.getClientMsgId());
            msg.setType(req.getType() == null ? "TEXT" : req.getType().toUpperCase());
            msg.setText(req.getText());
            msg.setStickerId(req.getStickerId());
            msg.setCreatedAt(LocalDateTime.now());
            saved = messageJpaRepo.save(msg);

            attachmentDtos = new ArrayList<>();
            if (req.getAttachments() != null) {
                for (AttachmentDto a : req.getAttachments()) {
                    MessageAttachmentJpa att = new MessageAttachmentJpa();
                    att.setMessageId(saved.getMessageId());
                    att.setUrl(a.getUrl());
                    att.setKind(a.getKind() == null ? "FILE" : a.getKind().toUpperCase());
                    att.setName(a.getName());
                    att.setSize(a.getSize());
                    att.setMimeType(a.getMimeType());
                    att.setWidth(a.getWidth());
                    att.setHeight(a.getHeight());
                    att.setDurationSec(a.getDurationSec());
                    att.setThumbnailUrl(a.getThumbnailUrl());
                    attachmentJpaRepo.save(att);
                    attachmentDtos.add(a);
                }
            }

            conversationJpaRepo.findById(conversationId).ifPresent(c -> {
                c.setUpdatedAt(LocalDateTime.now());
                conversationJpaRepo.save(c);
            });
        }

        MessageDto dto = toMessageDto(saved, attachmentDtos, participants);

        for (ConversationParticipantJpa p : participants) {
            simpMessagingTemplate.convertAndSendToUser(p.getStaffId().toString(), "/queue/chat-messages", dto);
        }

        // Web Push cho người nhận (trừ người gửi) — chỉ khi tin nhắn mới, tránh push
        // lại khi client gửi trùng clientMsgId. SW tự bỏ qua nếu tab người nhận đang mở.
        if (existing.isEmpty()) {
            List<Integer> recipients = participants.stream()
                    .map(ConversationParticipantJpa::getStaffId)
                    .filter(id -> id != null && !id.equals(senderId))
                    .toList();
            webPushDispatchService.sendChatMessage(recipients, dto.getSenderName(),
                    buildChatPreview(dto), conversationId);
        }

        return dto;
    }

    /** Tạo dòng xem trước ngắn gọn cho thông báo đẩy từ một tin nhắn chat. */
    private String buildChatPreview(MessageDto dto) {
        String text = dto.getText();
        if (StringUtils.hasText(text)) {
            String trimmed = text.trim();
            return trimmed.length() > 120 ? trimmed.substring(0, 120) + "…" : trimmed;
        }
        String type = dto.getType() == null ? "" : dto.getType().toLowerCase();
        return switch (type) {
            case "sticker" -> "Đã gửi một nhãn dán";
            case "image" -> "Đã gửi một hình ảnh";
            default -> {
                boolean hasAttachment = dto.getAttachments() != null && !dto.getAttachments().isEmpty();
                yield hasAttachment ? "Đã gửi một tệp đính kèm" : "Đã gửi một tin nhắn";
            }
        };
    }

    @Transactional
    public void markRead(Integer staffId, Integer conversationId, Integer upToMessageId) {
        ConversationParticipantJpa participant = participantJpaRepo.findByConversationIdAndStaffId(conversationId, staffId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.NOT_PARTICIPANT));

        if (upToMessageId != null
                && (participant.getLastReadMessageId() == null || upToMessageId > participant.getLastReadMessageId())) {
            participant.setLastReadMessageId(upToMessageId);
            participantJpaRepo.save(participant);
        }

        ChatReceiptDto receipt = new ChatReceiptDto(conversationId, upToMessageId, "read", staffId);
        participantJpaRepo.findByConversationId(conversationId).stream()
                .filter(p -> !p.getStaffId().equals(staffId))
                .forEach(p -> simpMessagingTemplate.convertAndSendToUser(p.getStaffId().toString(), "/queue/chat-receipts", receipt));
    }

    public void broadcastTyping(Integer conversationId, Integer staffId, boolean typing) {
        TypingEventDto event = new TypingEventDto(conversationId, staffId, typing);
        participantJpaRepo.findByConversationId(conversationId).stream()
                .filter(p -> !p.getStaffId().equals(staffId))
                .forEach(p -> simpMessagingTemplate.convertAndSendToUser(p.getStaffId().toString(), "/queue/chat-typing", event));
    }

    // ===================== Helpers =====================

    private List<AttachmentDto> attachmentsOf(Integer messageId) {
        return attachmentJpaRepo.findByMessageId(messageId).stream().map(this::toAttachmentDto).toList();
    }

    private AttachmentDto toAttachmentDto(MessageAttachmentJpa a) {
        return new AttachmentDto(a.getUrl(), a.getKind() == null ? null : a.getKind().toLowerCase(),
                a.getName(), a.getSize(), a.getMimeType(), a.getWidth(), a.getHeight(), a.getDurationSec(), a.getThumbnailUrl());
    }

    private MessageDto toMessageDto(ChatMessageJpa m, List<AttachmentDto> attachments, List<ConversationParticipantJpa> participants) {
        StaffProfile sender = staffProfileRepo.findById(m.getSenderId()).orElse(null);
        String status = computeStatus(m, participants);
        return new MessageDto(m.getMessageId(), m.getConversationId(), m.getClientMsgId(), m.getSenderId(),
                sender != null ? sender.getFullName() : null, sender != null ? sender.getAvatar() : null,
                m.getType() == null ? "text" : m.getType().toLowerCase(), m.getText(), attachments, m.getStickerId(),
                m.getCreatedAt(), status);
    }

    private String computeStatus(ChatMessageJpa m, List<ConversationParticipantJpa> participants) {
        List<ConversationParticipantJpa> others = participants.stream()
                .filter(p -> !p.getStaffId().equals(m.getSenderId())).toList();
        if (others.isEmpty()) return "sent";
        boolean allRead = others.stream().allMatch(p ->
                p.getLastReadMessageId() != null && p.getLastReadMessageId() >= m.getMessageId());
        return allRead ? "read" : "sent";
    }

    private void ensureParticipant(Integer conversationId, Integer staffId) {
        participantJpaRepo.findByConversationIdAndStaffId(conversationId, staffId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.NOT_PARTICIPANT));
    }

    private void ensureParticipant(List<ConversationParticipantJpa> participants, Integer staffId) {
        boolean isParticipant = participants.stream().anyMatch(p -> p.getStaffId().equals(staffId));
        if (!isParticipant) throw new ChatException(ChatErrorCode.NOT_PARTICIPANT);
    }
}
