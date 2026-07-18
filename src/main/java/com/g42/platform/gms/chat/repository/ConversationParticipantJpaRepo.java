package com.g42.platform.gms.chat.repository;

import com.g42.platform.gms.chat.entity.ConversationParticipantJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantJpaRepo extends JpaRepository<ConversationParticipantJpa, Integer> {

    List<ConversationParticipantJpa> findByStaffId(Integer staffId);

    List<ConversationParticipantJpa> findByConversationId(Integer conversationId);

    List<ConversationParticipantJpa> findByConversationIdIn(List<Integer> conversationIds);

    Optional<ConversationParticipantJpa> findByConversationIdAndStaffId(Integer conversationId, Integer staffId);
}
