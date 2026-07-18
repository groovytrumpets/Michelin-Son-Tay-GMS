package com.g42.platform.gms.chat.repository;

import com.g42.platform.gms.chat.entity.MessageAttachmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageAttachmentJpaRepo extends JpaRepository<MessageAttachmentJpa, Integer> {

    List<MessageAttachmentJpa> findByMessageId(Integer messageId);

    List<MessageAttachmentJpa> findByMessageIdIn(List<Integer> messageIds);
}
