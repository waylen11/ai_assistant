package com.huangyuanwei.aics.controller;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.huangyuanwei.aics.common.AppConstants;
import com.huangyuanwei.aics.common.Result;
import com.huangyuanwei.aics.dto.LoginRequest;
import com.huangyuanwei.aics.dto.LoginResponse;
import com.huangyuanwei.aics.dto.SaltResponse;
import com.huangyuanwei.aics.dto.UserInfoResponse;
import com.huangyuanwei.aics.entity.SysUser;
import com.huangyuanwei.aics.repository.SysUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证接口
 *
 * <h3>登录安全方案</h3>
 * <pre>
 *   1. 前端获取盐值 GET /auth/salt/{username}
 *   2. 前端计算哈希：clientHash = SHA256(salt + rawPassword)
 *   3. 前端 POST /auth/login 传入 clientHash
 *   4. 后端使用 BCrypt.checkpw(clientHash, dbPassword) 验证
 *      其中 dbPassword 是创建用户时 BCrypt.hashpw(clientHash) 的结果
 *
 *   注意：数据库存储的是 BCrypt(SHA256(salt + rawPassword))，
 *         即前端哈希后再经 BCrypt 加密存储，双重保护。
 * </pre>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserRepository userRepository;

    /**
     * 获取用户登录盐值（前端在登录前先调用此接口）
     */
    @GetMapping("/salt/{username}")
    public Result<SaltResponse> getSalt(@PathVariable String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        String salt = user.getSalt();
        if (salt == null || salt.isEmpty()) {
            // 兼容旧用户：使用默认盐值常量
            salt = AppConstants.LEGACY_SALT;
        }

        return Result.ok(new SaltResponse(salt));
    }

    /**
     * 登录接口
     *
     * @param req 登录请求：username + clientHash（前端已做 SHA256(salt+rawPassword)）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        String username = req.getUsername();
        String clientHash = req.getPassword(); // 前端已哈希：SHA256(salt + rawPassword)

        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!user.getEnabled()) {
            return Result.fail("账号已被禁用");
        }

        // 后端使用 BCrypt 验证：BCrypt.checkpw(前端哈希值, 数据库存储的BCrypt哈希)
        // 数据库存储的是：BCrypt(SHA256(salt + rawPassword))
        // 前端传来的是：SHA256(salt + rawPassword)
        // 因此直接用 BCrypt.checkpw 验证即可
        if (!BCrypt.checkpw(clientHash, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 登录
        StpUtil.login(user.getId(), new SaLoginModel().setExtra("username", user.getUsername()));

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        LoginResponse response = LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .permModel(user.getPermModel())
                .permKnowledge(user.getPermKnowledge())
                .permUser(user.getPermUser())
                .build();

        return Result.ok(response);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<UserInfoResponse> getInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        UserInfoResponse info = UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .permModel(user.getPermModel())
                .permKnowledge(user.getPermKnowledge())
                .permUser(user.getPermUser())
                .build();

        return Result.ok(info);
    }
}
