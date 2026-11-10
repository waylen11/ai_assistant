package com.huangyuanwei.aics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 聊天消息持久化实体
 */
@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "memory_id", nullable = false, length = 128)
    private String memoryId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    /**
     * 消息角色：user / assistant / system
     */
    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "tokens")
    private Integer tokens;

    /**
     * 来源：service=客服模式, assistant=助理模式
     */
    @Column(name = "source", nullable = false, length = 16)
    private String source = com.huangyuanwei.aics.enums.MessageSource.SERVICE.getValue();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
