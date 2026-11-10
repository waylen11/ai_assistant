package com.huangyuanwei.aics.common;

/**
 * 应用全局常量
 * 禁止在业务代码中直接使用魔法数字，必须在此处定义后引用
 */
public final class AppConstants {

    private AppConstants() {
        // 工具类，禁止实例化
    }

    // ==================== 密码相关 ====================

    /** 密码最小长度 */
    public static final int PASSWORD_MIN_LENGTH = 6;

    /** 密码最大长度 */
    public static final int PASSWORD_MAX_LENGTH = 20;

    /** 旧用户兼容默认盐值（历史原因，新用户不再使用此盐值） */
    public static final String LEGACY_SALT = "aics2026";

    // ==================== 消息/输入校验 ====================

    /** 聊天消息最大字符数 */
    public static final int CHAT_MESSAGE_MAX_LENGTH = 5000;

    /** 模型名称最大长度 */
    public static final int MODEL_NAME_MAX_LENGTH = 100;

    // ==================== 会话 ====================

    /** 新会话默认标题 */
    public static final String DEFAULT_SESSION_TITLE = "新会话";

    /** 自动生成会话标题的截取长度 */
    public static final int SESSION_TITLE_MAX_LENGTH = 20;

    // ==================== 知识库 ====================

    /** Milvus 向量集合名称 */
    public static final String MILVUS_COLLECTION_NAME = "ai_knowledge_base";

    /** 知识库 MinIO Bucket 默认名称 */
    public static final String KNOWLEDGE_BUCKET_DEFAULT = "knowledge-base";

    /** Chunk 预览：段落最小字符数（过滤过短段落） */
    public static final int CHUNK_PREVIEW_MIN_CHARS = 20;

    /** Chunk 预览：每条预览内容最大截取字符数 */
    public static final int CHUNK_PREVIEW_MAX_CHARS = 200;

    /** Chunk 预览：最多展示 Chunk 数量 */
    public static final int CHUNK_PREVIEW_MAX_COUNT = 50;

    /** 无扩展名文件默认扩展名 */
    public static final String FILE_DEFAULT_EXTENSION = "bin";
}
