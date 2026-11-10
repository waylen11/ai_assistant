package com.huangyuanwei.aics.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.huangyuanwei.aics.common.Result;
import com.huangyuanwei.aics.dto.CreateSessionRequest;
import com.huangyuanwei.aics.entity.ChatSession;
import com.huangyuanwei.aics.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 助理接口（需登录）
 */
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    /**
     * SSE 流式对话
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) String modelName) {
        return assistantService.streamChat(sessionId, message, modelName);
    }

    /**
     * 新建会话
     * 使用专门的 DTO，不再接受 Map 参数
     */
    @PostMapping("/sessions")
    public Result<ChatSession> createSession(@RequestBody(required = false) CreateSessionRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        String modelName = req != null ? req.getModelName() : null;
        return Result.ok(assistantService.createSession(userId, modelName));
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(assistantService.getSessions(userId));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<?> deleteSession(@PathVariable String sessionId) {
        assistantService.deleteSession(sessionId);
        return Result.ok();
    }
}
