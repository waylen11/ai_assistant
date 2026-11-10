package com.huangyuanwei.aics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 向量化状态枚举
 * 对应 knowledge_base.vector_status 字段
 */
@Getter
@AllArgsConstructor
public enum VectorStatus {

    /** 等待向量化 */
    PENDING("pending"),

    /** 向量化处理中 */
    PROCESSING("processing"),

    /** 向量化已完成 */
    COMPLETED("completed"),

    /** 向量化失败 */
    FAILED("failed");

    private final String value;
}
