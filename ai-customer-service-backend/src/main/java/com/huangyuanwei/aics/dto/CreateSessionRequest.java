package com.huangyuanwei.aics.dto;

import lombok.Data;

/**
 * 创建会话请求 DTO
 */
@Data
public class CreateSessionRequest {

    /** 使用的模型名称（可选，为空则使用系统默认模型） */
    private String modelName;
}
