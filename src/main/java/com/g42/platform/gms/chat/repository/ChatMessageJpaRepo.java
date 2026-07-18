package com.g42.platform.gms.chat.repository;

import com.g42.platform.gms.chat.entity.ChatMessageJpa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageJpaRepo extends JpaRepository<ChatMessageJpa, Integer> {

    Optional<ChatMessageJpa> findByConversationIdAndClientMsgId(Integer conversationId, String clientMsgId);

    Optional<ChatMessageJpa> findTopByConversationIdOrderByMessageIdDesc(Integer conversationId);

    @Query("SELECT m FROM ChatMessageJpa m WHERE m.conversationId = :conversationId "
            + "AND (:before IS NULL OR m.messageId < :before) ORDER BY m.messageId DESC")
    List<ChatMessageJpa> findPage(@Param("conversationId") Integer conversationId,
                                   @Param("before") Integer before,
                                   Pageable pageable);

    long countByConversationIdAndSenderIdNotAndMessageIdGreaterThan(
            Integer conversationId, Integer senderId, Integer messageId);

    long countByConversationIdAndSenderIdNot(Integer conversationId, Integer senderId);
}
