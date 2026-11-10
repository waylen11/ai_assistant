package com.huangyuanwei.aics.dto;

import lombok.Data;

/**
 * 流程图执行结果DTO
 */
@Data
public class GraphExecutionResultDTO {
    /**
     * 识别的意图
     */
    private String intent;
    
    /**
     * 检索的上下文
     */
    private String retrievedContext;
    
    /**
     * 下一步操作
     */
    private String nextAction;
}