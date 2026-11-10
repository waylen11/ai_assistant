package com.huangyuanwei.aics.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Column(name = "salt", length = 32)
    private String salt;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "gender", nullable = false)
    private Integer gender = 0;

    @Column(name = "department", length = 64)
    private String department;

    @Column(name = "position", length = 64)
    private String position;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    // 权限标志位（极简 RBAC）
    @Column(name = "perm_model", nullable = false)
    private Boolean permModel = false;

    @Column(name = "perm_knowledge", nullable = false)
    private Boolean permKnowledge = false;

    @Column(name = "perm_user", nullable = false)
    private Boolean permUser = false;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
