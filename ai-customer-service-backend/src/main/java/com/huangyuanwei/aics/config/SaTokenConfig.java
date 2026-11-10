package com.huangyuanwei.aics.config;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.huangyuanwei.aics.entity.SysUser;
import com.huangyuanwei.aics.enums.PermissionField;
import com.huangyuanwei.aics.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final SysUserRepository sysUserRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // String requestPath = SaHolder.getRequest().getRequestPath();
            // log.debug("请求路径: {}", requestPath);

            // ★ 放行所有 OPTIONS 预检请求（CORS 握手，不需要鉴权）
            SaRouter.match(SaHttpMethod.OPTIONS).stop();

            // 公开路由（无需登录）
            SaRouter.match("/chat/**").stop();
            SaRouter.match("/auth/**").stop();
            SaRouter.match("/models/enabled").stop();

            // 管理后台路由 - 模型管理（需登录+permModel权限）
            SaRouter.match("/admin/models/**")
                    .check(r -> checkPermission(PermissionField.PERM_MODEL));

            // 管理后台路由 - 知识库管理（需登录+permKnowledge权限）
            SaRouter.match("/admin/knowledge/**")
                    .check(r -> checkPermission(PermissionField.PERM_KNOWLEDGE));

            // 管理后台路由 - 用户管理（需登录+permUser权限）
            SaRouter.match("/admin/users/**")
                    .check(r -> checkPermission(PermissionField.PERM_USER));

            // AI 助理（需登录）
            SaRouter.match("/assistant/**").check(r -> StpUtil.checkLogin());

            // 其他所有接口需要登录
            SaRouter.match("/**").check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**")
           .excludePathPatterns(
               "/auth/login",
               "/auth/salt/*",
               "/chat/**",
               "/models/enabled",
               "/error"
           );
    }

    /**
     * 检查用户是否具有指定权限（使用枚举，禁止硬编码权限字段名字符串）
     */
    private void checkPermission(PermissionField permissionField) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NotPermissionException("用户不存在", null);
        }
        Boolean hasPerm = switch (permissionField) {
            case PERM_MODEL -> user.getPermModel();
            case PERM_KNOWLEDGE -> user.getPermKnowledge();
            case PERM_USER -> user.getPermUser();
        };
        if (!Boolean.TRUE.equals(hasPerm)) {
            throw new NotPermissionException("您没有权限执行此操作", null);
        }
    }
}
