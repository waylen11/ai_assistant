package com.huangyuanwei.aics.dto;

import lombok.Data;

/**
 * 流程图节点DTO
 */
@Data
public class GraphNodeDTO {
    /**
     * 节点ID
     */
    private String id;
    
    /**
     * 节点类型
     */
    private String type;
    
    /**
     * 节点标签
     */
    private String label;
    
    /**
     * 节点X坐标
     */
    private int x;
    
    /**
     * 节点Y坐标
     */
    private int y;
    
    /**
     * 节点描述
     */
    private String description;
}