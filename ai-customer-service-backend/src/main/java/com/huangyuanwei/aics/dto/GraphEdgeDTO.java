package com.huangyuanwei.aics.dto;

import lombok.Data;

/**
 * 流程图边DTO
 */
@Data
public class GraphEdgeDTO {
    /**
     * 边ID
     */
    private String id;
    
    /**
     * 源节点ID
     */
    private String source;
    
    /**
     * 目标节点ID
     */
    private String target;
    
    /**
     * 边标签
     */
    private String label;
    
    /**
     * 边条件
     */
    private String condition;
}