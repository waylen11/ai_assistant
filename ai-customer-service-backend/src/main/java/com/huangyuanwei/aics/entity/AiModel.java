package com.huangyuanwei.aics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AI 模型配置实体
 */
@Data
@Entity
@Table(name = "ai_model")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_type", nullable = false, length = 64)
    private String modelType;

    @Column(name = "model_name", nullable = false, length = 128, unique = true)
    private String modelName;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "api_key", nullable = false, length = 512)
    private String apiKey;

    @Column(name = "base_url", length = 512)
    private String baseUrl;

    @Column(name = "multimodal", nullable = false)
    private Boolean multimodal = false;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
