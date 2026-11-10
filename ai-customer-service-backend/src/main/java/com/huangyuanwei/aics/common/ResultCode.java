package com.huangyuanwei.aics.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 * 禁止在代码中直接使用 200/400/401/403/404/500 等魔法数字
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    OK(200, "success"),

    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),

    /** 未登录 / Token 失效 */
    UNAUTHORIZED(401, "请先登录"),

    /** 无操作权限 */
    FORBIDDEN(403, "没有操作权限"),

    /** 资源不存在 */
    NOT_FOUND(404, "请求的资源不存在"),

    /** 不支持的响应格式 */
    NOT_ACCEPTABLE(406, "不支持的响应格式"),

    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "系统内部错误，请稍后重试");

    private final int code;
    private final String message;
}
