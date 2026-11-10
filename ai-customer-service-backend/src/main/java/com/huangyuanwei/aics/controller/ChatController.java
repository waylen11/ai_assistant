package com.huangyuanwei.aics.controller;

import com.huangyuanwei.aics.common.Result;
import com.huangyuanwei.aics.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
    
/**
 * 公开 AI 客服聊天接口（无需登录）
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * SSE 流式聊天
     * GET /api/chat/stream?message=xxx&modelName=xxx
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String modelName,
            HttpServletRequest request) {
        // 以 IP 作为 memoryId
        String memoryId = getClientIp(request);
        return chatService.streamChat(memoryId, message, modelName);
    }

    /**
     * 获取历史消息
     */
    @GetMapping("/history")
    public Result<?> getHistory(HttpServletRequest request) {
        String memoryId = getClientIp(request);
        return Result.ok(chatService.getHistory(memoryId));
    }

    /**
     * 清除当前用户历史（可选功能）
     */
    @DeleteMapping("/history")
    public Result<?> clearHistory(HttpServletRequest request) {
        // 清除 Redis + MySQL（可选）
        return Result.ok();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
