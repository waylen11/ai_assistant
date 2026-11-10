package com.huangyuanwei.aics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huangyuanwei.aics.common.AppConstants;
import com.huangyuanwei.aics.config.ChatClientFactory;
import com.huangyuanwei.aics.dto.GraphContext;
import com.huangyuanwei.aics.dto.GraphExecutionResultDTO;
import com.huangyuanwei.aics.entity.AiModel;
import com.huangyuanwei.aics.entity.ChatMessage;
import com.huangyuanwei.aics.enums.MessageRole;
import com.huangyuanwei.aics.enums.MessageSource;
import com.huangyuanwei.aics.graph.CustomerServiceGraph;
import com.huangyuanwei.aics.repository.AiModelRepository;
import com.huangyuanwei.aics.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公开 AI 客服聊天服务
 * 使用用户 IP 作为 memoryId，Redis 缓存短期记忆，MySQL 持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClientFactory chatClientFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final AiModelRepository aiModelRepository;
    private final CustomerServiceGraph customerServiceGraph;
    private final ObjectMapper objectMapper;

    @Value("${app.memory.redis-prefix}")
    private String redisPrefix;

    @Value("${app.memory.max-messages}")
    private int maxMessages;

    @Value("${app.memory.ttl-hours}")
    private long ttlHours;

    private static final String SYSTEM_PROMPT = """
            # 角色定位
            你是一位专业、热情、高效的 AI 智能客服助手，代表公司为用户提供优质的客户服务体验。
            
            # 服务范围
            1. **产品咨询** - 详细介绍公司产品功能、技术规格、价格方案、使用场景等
            2. **在线预约** - 协助用户预约产品演示、技术咨询、商务洽谈等服务
            3. **企业信息** - 提供公司简介、发展历程、联系方式、办公地址等公开信息
            4. **售后支持** - 解答产品使用问题、故障排查、服务政策咨询
            5. **投诉建议** - 记录用户反馈，提供解决方案或转接人工客服
            
            # 沟通原则
            - **专业可信**：使用准确的产品术语，数据真实可靠
            - **热情友好**：使用礼貌用语，主动问候和道别
            - **简洁清晰**：避免冗长，重点突出，结构分明
            - **耐心细致**：理解用户需求，提供完整解答
            - **主动引导**：适时推荐相关产品或服务
            
            # 回复规范
            ## 列表格式
            当回复包含多个选项或步骤时，使用编号列表并换行：
            请提供以下信息以便我们为您安排：
            1. 您的姓名
            2. 公司名称（如果适用）
            3. 联系方式（电话或电子邮件）
            4. 预约时间偏好
            
            ## 表格格式
            当回复包含结构化数据（如产品对比、价格方案、联系信息等）时，必须使用标准 Markdown 表格格式：
            
            **标准表格格式示例：**
            | 产品版本 | 功能特性 | 价格 |
            |---------|---------|------|
            | 基础版 | 核心功能 | ¥999 |
            | 专业版 | 高级功能 | ¥2999 |
            
            **产品信息表格示例：**
            | 产品名称 | 功能特性 | 适用场景 |
            | :--- | :--- | :--- |
            | 智能办公系统 | 集成日程管理、邮件处理、文档协作等功能 | 适合企业内部团队协作和日常办公 |
            | 数据分析平台 | 提供数据可视化、报表生成、多数据源接入 | 适合需要数据分析和决策支持的企业 |
            | 云计算服务 | 高可用性、弹性扩展、安全可靠 | 适合需要灵活 IT 基础设施的企业 |
            
            ## 重要提示
            - **必须使用标准 Markdown 表格格式**，不要使用自定义格式（如 || 分隔符）
            - 表格列之间使用单个 | 分隔
            - 表头下方必须包含分隔行，格式为 |---|---|（根据列数调整）
            - 确保表格内容对齐，便于阅读
            - 表格前后必须有空行，确保正确渲染
            
            # 边界处理
            - 超出服务范围的问题：礼貌说明并提供替代方案（如转人工客服）
            - 敏感信息：不透露公司内部数据、用户隐私信息
            - 不确定的问题：诚实告知，承诺核实后回复
            - 紧急情况：引导用户拨打客服热线或联系人工客服
            
            # 开场白示例
            "您好！欢迎来到 [公司名称] 智能客服，我是您的专属客服助手。请问有什么可以帮助您的吗？"
            
            # 结束语示例
            "感谢您的咨询！如果您还有其他问题，随时欢迎联系我。祝您使用愉快！"
            """;

    /**
     * 流式聊天（SSE）
     *
     * @param memoryId  记忆ID（用户IP）
     * @param userInput 用户输入
     * @param modelName 选择的模型名称（可为空，使用默认模型）
     * @return 流式响应
     */
    public Flux<String> streamChat(String memoryId, String userInput, String modelName) {
        // 0. 输入校验（使用常量，禁止魔法数字）
        if (memoryId == null || memoryId.isBlank()) {
            throw new IllegalArgumentException("无效的用户标识");
        }
        if (userInput == null || userInput.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (userInput.length() > AppConstants.CHAT_MESSAGE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("消息内容过长，最多支持%d字符", AppConstants.CHAT_MESSAGE_MAX_LENGTH));
        }
        if (modelName != null && modelName.length() > AppConstants.MODEL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("模型名称过长");
        }

        // 1. 创建流程上下文
        GraphContext context = new GraphContext();
        context.setMemoryId(memoryId);
        context.setModelName(modelName);

        // 2. 执行客服流程编排
        GraphExecutionResultDTO executionResult = customerServiceGraph.execute(userInput, context);
        log.info("流程执行结果: 意图={}, 检索上下文={}, 下一步={}", 
                executionResult.getIntent(), executionResult.getRetrievedContext(), executionResult.getNextAction());

        // 3. 从 Redis 加载历史消息
        List<Message> history = loadHistory(memoryId);

        // 4. 构建消息列表
        history.add(new UserMessage(userInput));

        // 5. 构建 ChatClient（如指定模型则动态切换）
        ChatClient chatClient = buildChatClient(modelName);

        // 6. 构建系统提示，包含流程执行结果
        String enhancedSystemPrompt = buildEnhancedSystemPrompt(executionResult);

        // 7. 发起流式请求
        StringBuilder fullResponse = new StringBuilder();
        return chatClient.prompt()
                .system(enhancedSystemPrompt)
                .messages(history)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // 8. 流结束后更新 Redis 记忆 & 异步落库 MySQL
                    String responseText = fullResponse.toString();
                    saveToRedis(memoryId, userInput, responseText);
                    persistMessages(memoryId, null, userInput, responseText,
                            modelName, MessageSource.SERVICE.getValue());
                })
                .doOnError(e -> log.error("流式聊天异常 memoryId={}", memoryId, e));
    }

    /**
     * 构建增强的系统提示，包含流程执行结果
     */
    private String buildEnhancedSystemPrompt(GraphExecutionResultDTO executionResult) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
        
        // 添加流程执行结果到系统提示
        if (executionResult.getRetrievedContext() != null && !executionResult.getRetrievedContext().isBlank()) {
            prompt.append("\n\n根据以下检索到的信息来回答用户问题：\n");
            prompt.append(executionResult.getRetrievedContext());
        }
        
        return prompt.toString();
    }

    /**
     * 从 Redis 加载历史消息（最近 N 条）
     */
    private List<Message> loadHistory(String memoryId) {
        String key = redisPrefix + memoryId;
        List<String> rawList = stringRedisTemplate.opsForList().range(key, 0, -1);
        List<Message> messages = new ArrayList<>();
        if (rawList != null) {
            for (String jsonStr : rawList) {
                try {
                    Map<String, String> map = objectMapper.readValue(jsonStr,
                            new TypeReference<>() {
                            });
                    String role = map.get("role");
                    String content = map.get("content");
                    if (MessageRole.USER.getValue().equals(role)) {
                        messages.add(new UserMessage(content));
                    } else if (MessageRole.ASSISTANT.getValue().equals(role)) {
                        messages.add(new AssistantMessage(content));
                    }
                } catch (JsonProcessingException e) {
                    log.warn("解析历史消息失败: {}", e.getMessage());
                }
            }
        }
        return messages;
    }

    /**
     * 保存本轮对话到 Redis，并维护滑动窗口
     */
    private void saveToRedis(String memoryId, String userInput, String assistantReply) {
        String key = redisPrefix + memoryId;
        try {
            // 将 Map 序列化为 JSON 字符串存储
            String userJson = objectMapper.writeValueAsString(
                    Map.of("role", MessageRole.USER.getValue(), "content", userInput));
            String assistantJson = objectMapper.writeValueAsString(
                    Map.of("role", MessageRole.ASSISTANT.getValue(), "content", assistantReply));

            stringRedisTemplate.opsForList().rightPush(key, userJson);
            stringRedisTemplate.opsForList().rightPush(key, assistantJson);

            // 维护窗口大小：每对消息占2条，maxMessages*2 为上限
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size != null && size > maxMessages * 2L) {
                // 从头部删除最旧的2条
                stringRedisTemplate.opsForList().leftPop(key);
                stringRedisTemplate.opsForList().leftPop(key);
            }
            // 刷新 TTL
            stringRedisTemplate.expire(key, Duration.ofHours(ttlHours));
        } catch (JsonProcessingException e) {
            log.error("保存消息到 Redis 失败: {}", e.getMessage());
        }
    }

    /**
     * 异步持久化消息到 MySQL
     */
    @Async
    public void persistMessages(String memoryId, String sessionId,
                                 String userInput, String assistantReply,
                                 String modelName, String source) {
        try {
            ChatMessage userMsg = new ChatMessage();
            userMsg.setMemoryId(memoryId);
            userMsg.setSessionId(sessionId);
            userMsg.setRole(MessageRole.USER.getValue());
            userMsg.setContent(userInput);
            userMsg.setModelName(modelName);
            userMsg.setSource(source);
            chatMessageRepository.save(userMsg);

            ChatMessage assistantMsg = new ChatMessage();
            assistantMsg.setMemoryId(memoryId);
            assistantMsg.setSessionId(sessionId);
            assistantMsg.setRole(MessageRole.ASSISTANT.getValue());
            assistantMsg.setContent(assistantReply);
            assistantMsg.setModelName(modelName);
            assistantMsg.setSource(source);
            chatMessageRepository.save(assistantMsg);
        } catch (Exception e) {
            log.error("消息持久化失败 memoryId={}", memoryId, e);
        }
    }

    /**
     * 动态构建 ChatClient（支持多 Provider / 自定义 baseUrl 切换）
     * 将完整的 AiModel 传给工厂，工厂根据 baseUrl 决定用 DashScope 还是 OpenAI 兼容协议
     */
    private ChatClient buildChatClient(String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            try {
                AiModel model = aiModelRepository.findByModelNameAndEnabledTrue(modelName);
                if (model != null) {
                    log.info("切换到模型: {} (provider: {}, baseUrl: {})",
                            model.getModelName(), model.getProvider(), model.getBaseUrl());
                    return chatClientFactory.getChatClient(model);
                } else {
                    log.warn("未找到模型: {} 或模型未启用，使用默认模型", modelName);
                }
            } catch (Exception e) {
                log.warn("动态加载模型失败，使用默认模型: {}", e.getMessage());
            }
        }
        return chatClientFactory.getDefaultChatClient();
    }

    /**
     * 获取历史对话记录（用于前端展示）
     */
    public List<ChatMessage> getHistory(String memoryId) {
        return chatMessageRepository.findByMemoryIdOrderByCreatedAtAsc(memoryId);
    }
}
