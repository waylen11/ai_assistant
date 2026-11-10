package com.huangyuanwei.aics.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.DigestUtil;
import com.huangyuanwei.aics.common.AppConstants;
import com.huangyuanwei.aics.entity.SysUser;
import com.huangyuanwei.aics.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理服务
 *
 * <h3>密码存储方案</h3>
 * <pre>
 *   rawPassword ——[前端]——→ SHA256(salt + rawPassword) = clientHash
 *   clientHash  ——[后端]——→ BCrypt(clientHash) = dbPassword
 *   数据库存储 dbPassword
 *
 *   验证时：BCrypt.checkpw(clientHash, dbPassword)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;

    public List<SysUser> listAll() {
        return userRepository.findAll();
    }

    public SysUser getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /**
     * 创建用户
     * 密码存储：BCrypt(SHA256(salt + rawPassword))
     * 即先计算 clientHash，再用 BCrypt 加密存入数据库
     */
    @Transactional
    public SysUser create(SysUser user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        // 生成随机盐值
        String salt = IdUtil.fastSimpleUUID();
        user.setSalt(salt);

        // 将原始密码按照前端相同方式哈希，再 BCrypt 存储
        // 保证：数据库中的密码 = BCrypt(SHA256(salt + rawPassword))
        String rawPassword = user.getPassword();
        String clientHash = DigestUtil.sha256Hex(salt + rawPassword);
        user.setPassword(BCrypt.hashpw(clientHash));

        return userRepository.save(user);
    }

    @Transactional
    public SysUser update(Long id, SysUser req) {
        SysUser user = getById(id);
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        if (req.getPosition() != null) user.setPosition(req.getPosition());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void toggleEnabled(Long id) {
        SysUser user = getById(id);
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    /**
     * 重置密码（管理员操作）
     * 密码长度校验使用常量，不得硬编码数字
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null
                || newPassword.length() < AppConstants.PASSWORD_MIN_LENGTH
                || newPassword.length() > AppConstants.PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("密码长度必须在%d-%d个字符之间",
                            AppConstants.PASSWORD_MIN_LENGTH, AppConstants.PASSWORD_MAX_LENGTH));
        }
        SysUser user = getById(id);

        // 重置密码：重新生成盐值，保持与 create 相同的存储方式
        String salt = user.getSalt();
        if (salt == null || salt.isEmpty()) {
            salt = IdUtil.fastSimpleUUID();
            user.setSalt(salt);
        }
        String clientHash = DigestUtil.sha256Hex(salt + newPassword);
        user.setPassword(BCrypt.hashpw(clientHash));
        userRepository.save(user);
    }

    @Transactional
    public void assignPermissions(Long id, Boolean permModel, Boolean permKnowledge, Boolean permUser) {
        SysUser user = getById(id);
        user.setPermModel(permModel != null ? permModel : Boolean.FALSE);
        user.setPermKnowledge(permKnowledge != null ? permKnowledge : Boolean.FALSE);
        user.setPermUser(permUser != null ? permUser : Boolean.FALSE);
        userRepository.save(user);
    }
}
