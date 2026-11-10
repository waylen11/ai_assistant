package com.huangyuanwei.aics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名过长")
    private String username;

    /**
     * 前端传入的哈希后密码：SHA256(salt + rawPassword)
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
