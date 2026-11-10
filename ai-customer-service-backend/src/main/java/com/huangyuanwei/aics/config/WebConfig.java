package com.huangyuanwei.aics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Web 配置 - 使用 CorsFilter Bean 处理跨域
 *
 * <p>为什么用 CorsFilter 而不是 WebMvcConfigurer#addCorsMappings：
 * Sa-Token 的拦截器（Interceptor）会在 Spring MVC 处理 OPTIONS 预检请求之前返回 401/403，
 * 导致浏览器 CORS 预检失败。CorsFilter 是 Servlet Filter，优先级高于 Interceptor，
 * 能在 Sa-Token 介入之前完成 CORS 握手，OPTIONS 请求直接返回 200。</p>
 */
@Configuration
public class WebConfig {

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的来源（从配置文件读取）
        if (corsProperties.getAllowedOrigins() != null) {
            corsProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
        }
        // 本地开发兜底
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost");

        // 允许所有请求头和方法（含 OPTIONS 预检）
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // 允许携带凭证（Cookie / Authorization 头）
        config.setAllowCredentials(true);

        // OPTIONS 预检结果缓存 1 小时，减少预检次数
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
