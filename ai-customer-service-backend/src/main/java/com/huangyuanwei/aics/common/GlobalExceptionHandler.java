package com.huangyuanwei.aics.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理静态资源找不到异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("静态资源未找到: {}", e.getMessage());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 处理 HttpMediaTypeNotAcceptableException（无法生成可接受的响应格式）
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public Result<?> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.warn("不支持的媒体类型: {}", e.getMessage());
        return Result.fail(ResultCode.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLoginException(NotLoginException e) {
        return Result.unauthorized();
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermissionException(NotPermissionException e) {
        return Result.forbidden();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(ResultCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
