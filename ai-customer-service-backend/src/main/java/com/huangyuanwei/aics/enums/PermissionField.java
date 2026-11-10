package com.huangyuanwei.aics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权限字段枚举
 * 用于 Sa-Token 权限检查逻辑，避免硬编码权限字段名字符串
 */
@Getter
@AllArgsConstructor
public enum PermissionField {

    /** 模型管理权限 */
    PERM_MODEL("permModel"),

    /** 知识库管理权限 */
    PERM_KNOWLEDGE("permKnowledge"),

    /** 用户管理权限 */
    PERM_USER("permUser");

    private final String field;
}
