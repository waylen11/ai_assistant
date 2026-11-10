package com.huangyuanwei.aics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huangyuanwei.aics.common.AppConstants;
import com.huangyuanwei.aics.config.ChatClientFactory;
import com.huangyuanwei.aics.entity.AiModel;
import com.huangyuanwei.aics.entity.ChatSession;
import com.huangyuanwei.aics.enums.MessageRole;
import com.huangyuanwei.aics.enums.MessageSource;
import com.huangyuanwei.aics.repository.AiModelRepository;
import com.huangyuanwei.aics.repository.ChatMessageRepository;
import com.huangyuanwei.aics.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 助理服务（需登录）
 * 使用 sessionId 作为 memoryId，支持产品采购/入库/销售/出库等业务操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final ChatClientFactory chatClientFactory;
    private final AiModelRepository aiModelRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${app.memory.redis-prefix}")
    private String redisPrefix;

    @Value("${app.memory.max-messages}")
    private int maxMessages;

    @Value("${app.memory.ttl-hours}")
    private long ttlHours;

    private static final String SYSTEM_PROMPT = """
            # 角色定位
            你是一位专业、高效、可靠的企业 AI 智能助理，协助员工处理日常业务操作、数据查询和决策支持。你是员工的得力助手，致力于提升工作效率和业务准确性。
            
            # 核心职责
            ## 1. 产品采购管理
            - 记录采购需求：产品名称、规格型号、采购数量、预算金额
            - 管理供应商信息：供应商名称、联系方式、合作历史
            - 跟踪采购进度：待采购、采购中、已到货状态
            - 生成采购订单和采购汇总报表
            
            ## 2. 产品入库管理
            - 登记入库信息：产品名称、规格、入库数量、入库时间、仓库位置
            - 核对入库单据：采购单号、供应商、质检结果
            - 更新库存数据：实时同步库存数量和位置
            - 生成入库记录和库存变动报表
            
            ## 3. 产品销售管理
            - 处理销售订单：客户信息、产品明细、销售数量、销售价格
            - 管理客户档案：客户名称、联系方式、购买历史、信用等级
            - 跟踪订单状态：待处理、已确认、已发货、已完成
            - 生成销售订单和销售业绩报表
            
            ## 4. 产品出库管理
            - 登记出库信息：产品名称、规格、出库数量、出库时间、目的地
            - 核对出库单据：销售单号、客户信息、物流信息
            - 更新库存数据：实时扣减库存数量
            - 生成出库记录和物流跟踪报表
            
            ## 5. 业绩报表分析
            - 销售业绩统计：按时间、产品、客户、区域等维度分析
            - 采购汇总分析：采购成本、供应商对比、采购趋势
            - 库存状态监控：库存预警、周转率、滞销品分析
            - 利润分析报表：收入、成本、毛利、净利润
            
            # 工作原则
            - **准确性**：确保数据录入准确无误，反复核对关键信息
            - **高效性**：快速响应业务需求，简化操作流程
            - **完整性**：记录完整的业务信息，便于后续追溯和分析
            - **保密性**：保护企业商业数据和客户隐私信息
            - **专业性**：使用规范的业务术语，提供专业的业务建议
            
            # 输出规范
            ## 确认信息
            执行重要操作前，必须向用户确认关键信息：
            "请确认以下操作信息：
            - 操作类型：产品采购
            - 产品名称：XXX
            - 采购数量：XXX
            - 供应商：XXX
            确认无误后，我将为您执行该操作。"
            
            ## 列表格式
            当回复包含多个项目或步骤时，使用清晰的编号列表：
            今日待处理业务：
            1. 产品采购审批（3条）
            2. 入库登记（5条）
            3. 销售订单确认（2条）
            
            ## 表格格式
            当回复包含结构化数据时，必须使用 Markdown 表格：
            | 产品名称 | 规格型号 | 库存数量 | 预警状态 |
            |---------|---------|---------|---------|
            | 产品A | 标准版 | 150 | 正常 |
            | 产品B | 高级版 | 20 | 库存不足 |
            
            ## 重要提示
            - 表格列之间使用 | 分隔
            - 表头下方必须包含分隔行 |---|---|
            - 数据右对齐（数字）或左对齐（文本），便于阅读
            - 关键数据使用加粗或颜色标注
            
            # 业务处理流程
            1. **理解意图**：准确识别用户的业务需求（采购/入库/销售/出库/查询）
            2. **收集信息**：主动询问缺失的关键信息，确保数据完整
            3. **确认操作**：执行前向用户确认关键信息，避免误操作
            4. **执行处理**：按照业务流程执行相应操作
            5. **反馈结果**：清晰展示操作结果，包括成功/失败状态和详细信息
            6. **后续建议**：根据业务场景提供合理的后续操作建议
            
            # 边界处理
            - **权限不足**：提示用户联系管理员获取相应权限
            - **数据异常**：发现数据矛盾或异常时，及时提醒用户核实
            - **系统故障**：遇到技术问题，记录错误信息并建议联系技术支持
            - **超出范围**：对于非业务操作类请求，礼貌说明并提供替代方案
            - **紧急事务**：涉及紧急业务，建议立即联系相关负责人
            
            # 交互示例
            ## 开场白
            "您好！我是您的企业 AI 助理，随时准备协助您处理采购、入库、销售、出库等业务操作，以及生成各类业绩报表。请问今天有什么可以帮您的？"
            
            ## 操作完成
            "操作已成功完成！已为您记录 [产品A] 的采购信息，采购数量 100 件，供应商 XXX。您可以在采购记录中查看详情。还有其他需要处理的业务吗？"
            
            ## 数据查询
            "根据您的查询条件，本月的销售业绩如下：
            [表格展示]
            如需查看更详细的分析或其他时间段的报表，请告诉我。"
            
            ## 结束语
            "感谢您的使用！如有任何业务需求，随时召唤我。祝您工作顺利！"
            """;

    /**
     * 新建会话
     */
    public ChatSession createSession(Long userId, String modelName) {
        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle(AppConstants.DEFAULT_SESSION_TITLE);
        session.setModelName(modelName);
        return chatSessionRepository.save(session);
    }

    /**
     * 获取用户所有会话列表
     */
    public List<ChatSession> getSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByLastMessageAtDesc(userId);
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        chatSessionRepository.deleteBySessionId(sessionId);
        // 清除 Redis 记忆
        stringRedisTemplate.delete(redisPrefix + sessionId);
    }

    /**
     * 流式对话（AI助理）
     */
    public Flux<String> streamChat(String sessionId, String userInput, String modelName) {
        // 1. 加载 Redis 历史记忆
        List<Message> history = loadHistory(sessionId);
        history.add(new UserMessage(userInput));

        // 2. 动态构建 ChatClient（支持 DeepSeek 等第三方模型）
        ChatClient chatClient = buildChatClient(modelName);

        // 3. 流式响应
        StringBuilder fullResponse = new StringBuilder();
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .messages(history)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    String responseText = fullResponse.toString();
                    saveToRedis(sessionId, userInput, responseText);
                    updateSession(sessionId, userInput, modelName);
                    // 异步落库
                    persistMessages(sessionId, userInput, responseText, modelName);
                })
                .doOnError(e -> log.error("AI助理流式对话异常 sessionId={}", sessionId, e));
    }

    /**
     * 动态构建 ChatClient（支持多 Provider / 自定义 baseUrl 切换）
     */
    private ChatClient buildChatClient(String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            try {
                AiModel model = aiModelRepository.findByModelNameAndEnabledTrue(modelName);
                if (model != null) {
                    log.info("AI助理切换到模型: {} (provider: {}, baseUrl: {})",
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

    private List<Message> loadHistory(String sessionId) {
        String key = redisPrefix + sessionId;
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

    private void saveToRedis(String sessionId, String userInput, String assistantReply) {
        String key = redisPrefix + sessionId;
        try {
            String userJson = objectMapper.writeValueAsString(
                    Map.of("role", MessageRole.USER.getValue(), "content", userInput));
            String assistantJson = objectMapper.writeValueAsString(
                    Map.of("role", MessageRole.ASSISTANT.getValue(), "content", assistantReply));

            stringRedisTemplate.opsForList().rightPush(key, userJson);
            stringRedisTemplate.opsForList().rightPush(key, assistantJson);

            Long size = stringRedisTemplate.opsForList().size(key);
            if (size != null && size > maxMessages * 2L) {
                stringRedisTemplate.opsForList().leftPop(key);
                stringRedisTemplate.opsForList().leftPop(key);
            }
            stringRedisTemplate.expire(key, Duration.ofHours(ttlHours));
        } catch (JsonProcessingException e) {
            log.error("保存消息到 Redis 失败: {}", e.getMessage());
        }
    }

    private void updateSession(String sessionId, String firstMessage, String modelName) {
        chatSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            // 如果是第一条消息，自动更新标题
            if (AppConstants.DEFAULT_SESSION_TITLE.equals(session.getTitle())
                    && !firstMessage.isEmpty()) {
                session.setTitle(firstMessage.substring(0,
                        Math.min(AppConstants.SESSION_TITLE_MAX_LENGTH, firstMessage.length())));
            }
            session.setLastMessageAt(LocalDateTime.now());
            session.setMessageCount(session.getMessageCount() + 2);
            if (modelName != null) {
                session.setModelName(modelName);
            }
            chatSessionRepository.save(session);
        });
    }

    private void persistMessages(String sessionId, String userInput, String assistantReply, String modelName) {
        com.huangyuanwei.aics.entity.ChatMessage userMsg = new com.huangyuanwei.aics.entity.ChatMessage();
        userMsg.setMemoryId(sessionId);
        userMsg.setSessionId(sessionId);
        userMsg.setRole(MessageRole.USER.getValue());
        userMsg.setContent(userInput);
        userMsg.setModelName(modelName);
        userMsg.setSource(MessageSource.ASSISTANT.getValue());
        chatMessageRepository.save(userMsg);

        com.huangyuanwei.aics.entity.ChatMessage assistantMsg = new com.huangyuanwei.aics.entity.ChatMessage();
        assistantMsg.setMemoryId(sessionId);
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole(MessageRole.ASSISTANT.getValue());
        assistantMsg.setContent(assistantReply);
        assistantMsg.setModelName(modelName);
        assistantMsg.setSource(MessageSource.ASSISTANT.getValue());
        chatMessageRepository.save(assistantMsg);
    }
}
