package com.huangyuanwei.aics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息角色枚举
 * 对应 chat_message.role 字段
 */
@Getter
@AllArgsConstructor
public enum MessageRole {

    /** 用户消息 */
    USER("user"),

    /** AI 助手消息 */
    ASSISTANT("assistant"),

    /** 系统提示消息 */
    SYSTEM("system");

    private final String value;
}
