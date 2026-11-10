package com.huangyuanwei.aics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 流程编排图实体
 */
@Data
@Entity
@Table(name = "workflow_graph")
public class WorkflowGraph {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "graph_name", nullable = false, length = 128)
    private String graphName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "graph_json", nullable = false, columnDefinition = "LONGTEXT")
    private String graphJson;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "graph_type", nullable = false, length = 32)
    private String graphType = "customer_service";

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
