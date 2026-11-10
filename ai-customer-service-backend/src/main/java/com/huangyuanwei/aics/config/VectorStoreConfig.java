package com.huangyuanwei.aics.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;

/**
 * 向量存储配置
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.milvus.client.host}")
    private String milvusHost;

    @Value("${spring.ai.vectorstore.milvus.client.port}")
    private int milvusPort;

    @Value("${spring.ai.vectorstore.milvus.client.username}")
    private String milvusUsername;

    @Value("${spring.ai.vectorstore.milvus.client.password}")
    private String milvusPassword;

    @Value("${spring.ai.vectorstore.milvus.databaseName}")
    private String databaseName;

    @Value("${spring.ai.vectorstore.milvus.collectionName}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.embeddingDimension}")
    private int embeddingDimension;

    @Value("${spring.ai.vectorstore.milvus.initializeSchema}")
    private boolean initializeSchema;

    /**
     * 当 Milvus 启用时，创建真实的 VectorStore Bean
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.vectorstore.milvus.enabled", havingValue = "true")
    public VectorStore milvusVectorStore(@Autowired EmbeddingModel embeddingModel) {
        MilvusServiceClient milvusClient = createMilvusClient();
        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .databaseName(databaseName)
                .collectionName(collectionName)
                .embeddingDimension(embeddingDimension)
                .initializeSchema(initializeSchema)
                .build();
    }

    /**
     * 当 Milvus 禁用时，创建一个空的 VectorStore Bean 避免启动失败
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.vectorstore.milvus.enabled", havingValue = "false")
    public VectorStore dummyVectorStore() {
        return new VectorStore() {
            @Override
            public void add(@NotNull List<Document> documents) {
                // 空实现
            }

            @Override
            public void delete(@NotNull List<String> idList) {

            }

            @Override
            public void delete(@NotNull Filter.Expression filterExpression) {

            }

            @NotNull
            @Override
            public List<Document> similaritySearch(@NotNull SearchRequest request) {
                return Collections.emptyList();
            }
        };
    }

    private MilvusServiceClient createMilvusClient() {
        ConnectParam param = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .withAuthorization(milvusUsername, milvusPassword)
                .withConnectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        return new MilvusServiceClient(param);
    }

    // ==================== ChatClient 配置 ====================

    /**
     * 默认的 ChatClient（阿里云百炼 DashScope）
     * 使用 @Qualifier("defaultChatClient") 注入到 ChatClientFactory
     */
    @Bean
    @Primary
    @Qualifier("defaultChatClient")
    public ChatClient defaultChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

}
