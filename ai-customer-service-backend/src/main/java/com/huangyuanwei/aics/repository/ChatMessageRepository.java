package com.huangyuanwei.aics.repository;

import com.huangyuanwei.aics.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByMemoryIdOrderByCreatedAtAsc(String memoryId);

    List<ChatMessage> findByMemoryIdOrderByCreatedAtDesc(String memoryId, Pageable pageable);

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    long countByMemoryId(String memoryId);

    void deleteByMemoryId(String memoryId);
}
