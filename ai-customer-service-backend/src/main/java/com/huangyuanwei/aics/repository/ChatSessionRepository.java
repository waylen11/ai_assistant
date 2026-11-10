package com.huangyuanwei.aics.repository;

import com.huangyuanwei.aics.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdOrderByLastMessageAtDesc(Long userId);
    Optional<ChatSession> findBySessionId(String sessionId);
    void deleteBySessionId(String sessionId);
}
