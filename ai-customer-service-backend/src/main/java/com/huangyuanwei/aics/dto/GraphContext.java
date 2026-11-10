package com.huangyuanwei.aics.dto;

import lombok.Data;

/**
 * 流程图执行上下文DTO
 */
@Data
public class GraphContext {
    /**
     * 会话内存ID
     */
    private String memoryId;
    
    /**
     * AI模型名称
     */
    private String modelName;
    
    /**
     * 其他上下文信息
     */
    private String extraInfo;
}