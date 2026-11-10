package com.huangyuanwei.aics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息来源枚举
 * 对应 chat_message.source 字段，区分客服模式和助理模式
 */
@Getter
@AllArgsConstructor
public enum MessageSource {

    /** 来自公开客服模式（无需登录） */
    SERVICE("service"),

    /** 来自 AI 助理模式（需登录） */
    ASSISTANT("assistant");

    private final String value;
}
