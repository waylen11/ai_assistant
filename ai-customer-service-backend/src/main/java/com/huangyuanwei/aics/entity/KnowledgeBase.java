package com.huangyuanwei.aics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 32)
    private String fileType;

    @Column(name = "minio_bucket", nullable = false, length = 64)
    private String minioBucket = com.huangyuanwei.aics.common.AppConstants.KNOWLEDGE_BUCKET_DEFAULT;

    @Column(name = "minio_path", nullable = false, length = 512)
    private String minioPath;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount = 0;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize = 500;

    @Column(name = "chunk_overlap", nullable = false)
    private Integer chunkOverlap = 50;

    /**
     * 向量化状态：pending / processing / completed / failed
     */
    @Column(name = "vector_status", nullable = false, length = 16)
    private String vectorStatus = com.huangyuanwei.aics.enums.VectorStatus.PENDING.getValue();

    @Column(name = "vector_collection", length = 128)
    private String vectorCollection;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "upload_user_id")
    private Long uploadUserId;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
