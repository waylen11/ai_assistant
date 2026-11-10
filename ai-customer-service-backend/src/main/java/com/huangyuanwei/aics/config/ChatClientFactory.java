package com.huangyuanwei.aics.config;

import com.huangyuanwei.aics.entity.AiModel;
import com.huangyuanwei.aics.repository.AiModelRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatClient 工厂类
 * 根据数据库中的 AiModel 配置（provider / baseUrl / apiKey）动态构建并缓存 ChatClient。
 *
 * 启动时从数据库 sort_order 最高的有效模型构建默认 ChatClient，
 * 不依赖 application.yml 的硬编码模型参数。
 */
@Slf4j
@Component
public class ChatClientFactory {

    /**
     * Spring AI 自动装配的 DashScope ChatClient.Builder（由 spring-ai-alibaba-starter 提供）
     */
    private final ChatClient.Builder dashScopeClientBuilder;

    /**
     * 默认 ChatClient —— 启动后由 @PostConstruct 从数据库加载，初始值为 DashScope 裸 Builder
     */
    private volatile ChatClient defaultChatClient;

    /**
     * 记录当前默认模型名称，方便日志追踪
     */
    private volatile String defaultModelName = "(未初始化)";

    /**
     * 按 modelName 缓存动态构建的 ChatClient，避免重复创建
     */
    private final Map<String, ChatClient> clientCache = new ConcurrentHashMap<>();

    private final AiModelRepository aiModelRepository;

    public ChatClientFactory(
            @Qualifier("defaultChatClient") ChatClient defaultChatClient,
            ChatClient.Builder dashScopeClientBuilder,
            AiModelRepository aiModelRepository) {
        this.defaultChatClient = defaultChatClient;
        this.dashScopeClientBuilder = dashScopeClientBuilder;
        this.aiModelRepository = aiModelRepository;
    }

    /**
     * 应用启动完成后，从数据库取 sort_order 最高的启用模型作为默认 ChatClient。
     * 如果数据库里没有有效模型（首次部署数据库为空），则保留 DashScope 裸 Client 兜底。
     */
    @PostConstruct
    public void initDefaultClient() {
        try {
            aiModelRepository.findFirstByEnabledTrueOrderBySortOrderDesc().ifPresentOrElse(
                    model -> {
                        this.defaultChatClient = buildClientForModel(model);
                        this.defaultModelName = model.getModelName();
                        log.info("默认模型已从数据库加载: {} (provider: {}, hasBaseUrl: {})",
                                model.getModelName(), model.getProvider(),
                                StringUtils.hasText(model.getBaseUrl()));
                    },
                    () -> log.warn("数据库中没有启用的模型，将使用 DashScope 裸 Client 兜底，请尽快在管理后台配置模型")
            );
        } catch (Exception e) {
            // 数据库连接失败等场景下不影响启动，使用兜底 Client
            log.error("初始化默认模型失败，使用兜底 Client: {}", e.getMessage());
        }
    }

    /**
     * 获取当前默认 ChatClient
     */
    public ChatClient getDefaultChatClient() {
        return defaultChatClient;
    }

    /**
     * 根据 AiModel 配置动态获取 ChatClient。
     * <p>
     * 规则：
     * 1. model 为 null → 返回当前默认 Client
     * 2. baseUrl 为空 → 使用 DashScope ChatClient（仅切换模型名，通过 options 覆盖）
     * 3. baseUrl 非空 → 使用 OpenAiApi 构建自定义 ChatClient（支持 DeepSeek 等第三方）
     *
     * @param model 数据库中的 AiModel 配置（可为 null）
     * @return 对应的 ChatClient
     */
    public ChatClient getChatClient(AiModel model) {
        if (model == null) {
            return defaultChatClient;
        }

        String modelName = model.getModelName();
        log.debug("获取 ChatClient - model: {}, provider: {}, hasBaseUrl: {}",
                modelName, model.getProvider(), StringUtils.hasText(model.getBaseUrl()));

        // 无 baseUrl → DashScope 系列，使用缓存或构建 DashScope Client
        if (!StringUtils.hasText(model.getBaseUrl())) {
            return clientCache.computeIfAbsent(modelName, key -> buildDashScopeClient(model));
        }

        // 有自定义 baseUrl（如 DeepSeek、OpenAI），按 modelName 缓存动态 Client
        return clientCache.computeIfAbsent(modelName, key -> {
            log.info("构建新的 OpenAI 兼容 ChatClient: model={}, baseUrl={}", key, model.getBaseUrl());
            return buildOpenAiCompatibleClient(model.getBaseUrl(), model.getApiKey(), key);
        });
    }

    /**
     * 重新从数据库刷新默认 Client（管理后台切换默认模型时调用）
     */
    public void refreshDefaultClient() {
        initDefaultClient();
    }

    // ──────────────────────────────────────────────────────────────
    //  私有构建方法
    // ──────────────────────────────────────────────────────────────

    /**
     * 根据 AiModel 选择合适的构建方法
     */
    private ChatClient buildClientForModel(AiModel model) {
        if (StringUtils.hasText(model.getBaseUrl())) {
            return buildOpenAiCompatibleClient(model.getBaseUrl(), model.getApiKey(), model.getModelName());
        }
        return buildDashScopeClient(model);
    }

    /**
     * 构建 DashScope ChatClient，通过 defaultOptions 指定模型名
     */
    private ChatClient buildDashScopeClient(AiModel model) {
        return dashScopeClientBuilder
                .defaultOptions(
                        org.springframework.ai.chat.prompt.ChatOptions.builder()
                                .model(model.getModelName())
                                .temperature(0.7)
                                .build()
                )
                .build();
    }

    /**
     * 使用 OpenAiApi 构建兼容 OpenAI 协议的 ChatClient
     * 适用于 DeepSeek、OpenAI、Moonshot、智谱等任何兼容 OpenAI 接口的模型
     */
    private ChatClient buildOpenAiCompatibleClient(String baseUrl, String apiKey, String modelName) {
        String normalizedUrl = normalizeBaseUrl(baseUrl);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedUrl)
                .apiKey(StringUtils.hasText(apiKey) ? apiKey : "empty")
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(0.7)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(chatModel).build();
    }

    /**
     * 规范化 baseUrl：去掉末尾斜杠和 /v1（OpenAiApi 内部会自动拼接 /v1/chat/completions）
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) return baseUrl;
        String url = baseUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/v1")) url = url.substring(0, url.length() - 3);
        return url;
    }

    /**
     * 清除指定 model 的缓存（当模型配置更新时调用）
     */
    public void evictCache(String modelName) {
        clientCache.remove(modelName);
        log.info("已清除模型缓存: {}", modelName);
    }

    /**
     * 清除所有缓存
     */
    public void clearCache() {
        clientCache.clear();
        log.info("已清除所有模型缓存");
    }

    public String getDefaultModelName() {
        return defaultModelName;
    }
}
