package com.huangyuanwei.aics.common;

import lombok.Data;

/**
 * 统一 API 响应包装类
 * 状态码统一使用 {@link ResultCode} 枚举，禁止直接传入魔法数字
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private Result() {}

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.OK.getCode());
        result.setMessage(ResultCode.OK.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.INTERNAL_ERROR.getCode());
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(message);
        return result;
    }

    /**
     * @deprecated 请使用 {@link #fail(ResultCode)} 替代，避免魔法数字
     */
    @Deprecated
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> unauthorized() {
        return fail(ResultCode.UNAUTHORIZED);
    }

    public static <T> Result<T> forbidden() {
        return fail(ResultCode.FORBIDDEN);
    }
}
