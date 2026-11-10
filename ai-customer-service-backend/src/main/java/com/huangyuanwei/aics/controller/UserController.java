package com.huangyuanwei.aics.controller;

import com.huangyuanwei.aics.common.Result;
import com.huangyuanwei.aics.dto.AssignPermissionsRequest;
import com.huangyuanwei.aics.dto.ResetPasswordRequest;
import com.huangyuanwei.aics.entity.SysUser;
import com.huangyuanwei.aics.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<List<SysUser>> list() {
        return Result.ok(userService.listAll());
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<SysUser> create(@RequestBody SysUser user) {
        return Result.ok(userService.create(user));
    }

    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        return Result.ok(userService.update(id, user));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PatchMapping("/{id}/toggle")
    public Result<?> toggle(@PathVariable Long id) {
        userService.toggleEnabled(id);
        return Result.ok();
    }

    /**
     * 重置密码接口
     * 使用专门的 DTO，不再接受 Map 参数
     */
    @PatchMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req.getNewPassword());
        return Result.ok();
    }

    /**
     * 分配权限接口
     * 使用专门的 DTO，不再接受 Map 参数
     */
    @PatchMapping("/{id}/permissions")
    public Result<?> assignPermissions(@PathVariable Long id, @RequestBody AssignPermissionsRequest req) {
        userService.assignPermissions(id, req.getPermModel(), req.getPermKnowledge(), req.getPermUser());
        return Result.ok();
    }
}
