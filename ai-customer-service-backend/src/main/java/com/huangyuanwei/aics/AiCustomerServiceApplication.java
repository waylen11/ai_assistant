package com.huangyuanwei.aics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI 智能客服系统启动类
 */
@EnableConfigurationProperties
@SpringBootApplication
@EnableAsync
public class AiCustomerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCustomerServiceApplication.class, args);
    }
}
