package com.huangyuanwei.aics.enums;

import lombok.Getter;

/**
 * 流程图常量枚举类
 */
@Getter
public enum GraphConstants {
    // 节点ID
    NODE_INTENT("intent_recognition", "意图识别"),
    NODE_KNOWLEDGE("knowledge_retrieval", "知识检索"),
    NODE_BUSINESS("business_execution", "业务执行"),
    NODE_REPLY("reply_generation", "回复生成"),
    
    // 边条件
    EDGE_KNOWLEDGE("knowledge", "知识查询"),
    EDGE_BUSINESS("business", "业务操作"),
    
    // 节点类型
    NODE_TYPE_INTENT("intent_recognition", "意图识别节点"),
    NODE_TYPE_KNOWLEDGE("knowledge_retrieval", "知识检索节点"),
    NODE_TYPE_BUSINESS("business_execution", "业务执行节点"),
    NODE_TYPE_REPLY("reply_generation", "回复生成节点"),
    
    // 图结构键
    GRAPH_NODES("nodes", "节点列表"),
    GRAPH_EDGES("edges", "边列表"),
    
    // 节点属性键
    NODE_ID("id", "节点ID"),
    NODE_TYPE("type", "节点类型"),
    NODE_LABEL("label", "节点标签"),
    NODE_X("x", "节点X坐标"),
    NODE_Y("y", "节点Y坐标"),
    NODE_DESCRIPTION("description", "节点描述"),
    
    // 边属性键
    EDGE_ID("id", "边ID"),
    EDGE_SOURCE("source", "边源节点"),
    EDGE_TARGET("target", "边目标节点"),
    EDGE_LABEL("label", "边标签"),
    EDGE_CONDITION("condition", "边条件"),
    
    // 执行结果键
    EXECUTION_INTENT("intent", "意图"),
    EXECUTION_RETRIEVED_CONTEXT("retrievedContext", "检索上下文"),
    EXECUTION_NEXT_ACTION("nextAction", "下一步操作");
    
    private final String value;
    private final String description;
    
    GraphConstants(String value, String description) {
        this.value = value;
        this.description = description;
    }

}