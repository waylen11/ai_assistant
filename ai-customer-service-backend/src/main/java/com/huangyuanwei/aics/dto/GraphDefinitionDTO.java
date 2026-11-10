package com.huangyuanwei.aics.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程图定义DTO
 */
@Data
public class GraphDefinitionDTO {
    /**
     * 节点列表
     */
    private List<GraphNodeDTO> nodes;
    
    /**
     * 边列表
     */
    private List<GraphEdgeDTO> edges;
}