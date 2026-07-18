package com.g42.platform.gms.chat.repository;

import com.g42.platform.gms.chat.entity.ConversationJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationJpaRepo extends JpaRepository<ConversationJpa, Integer> {
}
