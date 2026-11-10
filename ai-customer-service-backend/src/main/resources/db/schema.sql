-- ============================================================
-- AI 智能客服系统 - 数据库设计
-- 数据库版本：MySQL 8.0+
-- 字符集：utf8mb4
-- 创建时间：2026-03-26
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_customer_service
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_customer_service;

-- ============================================================
-- 1. 模型配置表 ai_model
--    存储系统支持的所有 AI 大模型配置信息
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_model` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `model_type`      VARCHAR(64)      NOT NULL                COMMENT '大模型类型，如 qwen/gpt/deepseek',
    `model_name`      VARCHAR(128)     NOT NULL                COMMENT '模型名称，如 qwen-max',
    `provider`        VARCHAR(64)      NOT NULL                COMMENT '提供厂商，如 阿里云/OpenAI',
    `description`     TEXT                                     COMMENT '大模型优势描述',
    `api_key`         VARCHAR(512)     NOT NULL                COMMENT 'API Key（加密存储）',
    `base_url`        VARCHAR(512)                             COMMENT '自定义 API Base URL',
    `multimodal`      TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '是否支持多模态：0否 1是',
    `icon_url`        VARCHAR(512)                             COMMENT '模型图标 URL',
    `enabled`         TINYINT(1)       NOT NULL DEFAULT 1      COMMENT '是否启用：0禁用 1启用',
    `sort_order`      INT              NOT NULL DEFAULT 0      COMMENT '排序权重，越大越靠前',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_name` (`model_name`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型配置表';

-- 初始化默认模型
INSERT INTO `ai_model` (`model_type`, `model_name`, `provider`, `description`, `api_key`, `base_url`, `multimodal`, `icon_url`, `enabled`, `sort_order`)
VALUES
    ('qwen', 'qwen-max',     '阿里云百炼', '通义千问旗舰模型，支持长文本推理，适用于复杂业务场景', 'your-api-key-here', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0, '/icons/qwen.svg',    1, 100),
    ('qwen', 'qwen-plus',    '阿里云百炼', '通义千问增强模型，性价比高，适用于日常对话',           'your-api-key-here', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0, '/icons/qwen.svg',    1, 90),
    ('qwen', 'qwen-vl-max',  '阿里云百炼', '通义千问视觉语言模型，支持图片理解与分析',             'your-api-key-here', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 1, '/icons/qwen-vl.svg', 1, 80),
    ('qwen', 'qwen-turbo',   '阿里云百炼', '通义千问极速版，响应速度快，适用于实时对话',           'your-api-key-here', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0, '/icons/qwen.svg',    1, 70),
    ('deepseek', 'deepseek-chat', 'DeepSeek', 'DeepSeek 旗舰对话模型，推理能力强，支持代码与复杂任务', 'your-api-key-here', 'https://api.deepseek.com', 0, '/icons/deepseek.svg', 1, 60)
ON DUPLICATE KEY UPDATE
    `description` = VALUES(`description`),
    `icon_url`    = VALUES(`icon_url`),
    `sort_order`  = VALUES(`sort_order`);

-- 修复存量数据中旧的 .png 图标路径
UPDATE `ai_model` SET `icon_url` = '/icons/qwen.svg'     WHERE `icon_url` = '/icons/qwen.png';
UPDATE `ai_model` SET `icon_url` = '/icons/qwen-vl.svg'  WHERE `icon_url` = '/icons/qwen-vl.png';
UPDATE `ai_model` SET `icon_url` = '/icons/deepseek.svg' WHERE `icon_url` = '/icons/deepseek.png';




-- ============================================================
-- 2. 系统用户表 sys_user
--    存储系统内部用户信息及权限标志位
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`        VARCHAR(64)      NOT NULL                COMMENT '用户名（唯一）',
    `password`        VARCHAR(256)     NOT NULL                COMMENT '密码（SHA-256 哈希）',
    `salt`            VARCHAR(32)                              COMMENT '密码盐值',
    `nickname`        VARCHAR(64)                              COMMENT '昵称',
    `avatar_url`      VARCHAR(512)                             COMMENT '头像 URL（存 MinIO）',
    `gender`          INT(1)           NOT NULL DEFAULT 0      COMMENT '性别：0未知 1男 2女',
    `department`      VARCHAR(64)                              COMMENT '部门',
    `position`        VARCHAR(64)                              COMMENT '岗位',
    `email`           VARCHAR(128)                             COMMENT '邮箱',
    `phone`           VARCHAR(20)                              COMMENT '手机号',
    -- 权限标志位（极简 RBAC，无需额外角色表）
    `perm_model`      TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '模型管理权限：0否 1是',
    `perm_knowledge`  TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '知识库管理权限：0否 1是',
    `perm_user`       TINYINT(1)       NOT NULL DEFAULT 0      COMMENT '用户管理权限：0否 1是',
    `enabled`         TINYINT(1)       NOT NULL DEFAULT 1      COMMENT '账号状态：0停用 1启用',
    `last_login_at`   DATETIME                                 COMMENT '最后登录时间',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 初始化超级管理员（密码：admin123）
-- 密码存储方案：BCrypt(SHA256(salt + rawPassword))
--   salt        = 'aics2026'（默认兼容盐值）
--   clientHash  = SHA256('aics2026' + 'admin123') = 1c8337e449f53a356ef38715a00c0779c093145819e70383562586d5c6d0c6a8
--   dbPassword  = BCrypt(clientHash)
-- 注意：如需重新初始化，请先删除已有记录：DELETE FROM sys_user WHERE username='admin';
INSERT INTO `sys_user` (`username`, `password`, `salt`, `nickname`, `gender`, `department`, `position`, `perm_model`, `perm_knowledge`, `perm_user`, `enabled`)
VALUES ('admin', '$2b$12$8gGBQGVCvfbKfro/nnKF6OOBaMXd/jWXfXjF8y.C5OjmMNd/eNaP6', 'aics2026', '超级管理员', 1, '技术部', '系统管理员', 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
    password = '$2b$12$8gGBQGVCvfbKfro/nnKF6OOBaMXd/jWXfXjF8y.C5OjmMNd/eNaP6',
    salt = 'aics2026';


-- ============================================================
-- 3. 聊天会话表 chat_session
--    AI 助理模式下的会话管理（模块二专用）
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id`      VARCHAR(64)      NOT NULL                COMMENT '会话唯一标识（UUID，作为 memoryId）',
    `user_id`         BIGINT UNSIGNED  NOT NULL                COMMENT '所属用户ID',
    `title`           VARCHAR(128)     NOT NULL DEFAULT '新会话' COMMENT '会话标题（取第一条消息前20字）',
    `model_name`      VARCHAR(128)                             COMMENT '本次会话使用的模型名称',
    `message_count`   INT              NOT NULL DEFAULT 0      COMMENT '消息数量',
    `last_message_at` DATETIME                                 COMMENT '最后消息时间',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_last_message_at` (`last_message_at`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表（AI助理模式）';


-- ============================================================
-- 4. 聊天消息表 chat_message
--    持久化存储所有聊天消息（含 AI 客服和 AI 助理两种模式）
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `memory_id`       VARCHAR(128)     NOT NULL                COMMENT '记忆ID：客服模式为用户IP，助理模式为sessionId',
    `session_id`      VARCHAR(64)                              COMMENT '关联会话ID（AI助理模式专用）',
    `role`            VARCHAR(16)      NOT NULL                COMMENT '消息角色：user/assistant/system',
    `content`         LONGTEXT         NOT NULL                COMMENT '消息内容',
    `model_name`      VARCHAR(128)                             COMMENT '使用的模型名称',
    `tokens`          INT                                      COMMENT '消耗的 Token 数',
    `source`          VARCHAR(16)      NOT NULL DEFAULT 'service' COMMENT '来源：service=客服 assistant=助理',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_memory_id` (`memory_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_source_role` (`source`, `role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息持久化表';


-- ============================================================
-- 5. 知识库表 knowledge_base
--    管理上传至 MinIO 并向量化到 Milvus 的文档
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name`       VARCHAR(256)     NOT NULL                COMMENT '原始文件名',
    `file_size`       BIGINT                                   COMMENT '文件大小（字节）',
    `file_type`       VARCHAR(32)                              COMMENT '文件类型：pdf/docx/txt/md',
    `minio_bucket`    VARCHAR(64)      NOT NULL DEFAULT 'knowledge' COMMENT 'MinIO Bucket 名称',
    `minio_path`      VARCHAR(512)     NOT NULL                COMMENT 'MinIO 存储路径',
    `chunk_count`     INT              NOT NULL DEFAULT 0      COMMENT '分块数量',
    `chunk_size`      INT              NOT NULL DEFAULT 500    COMMENT '分块大小（字符数）',
    `chunk_overlap`   INT              NOT NULL DEFAULT 50     COMMENT '分块重叠字符数',
    `vector_status`   VARCHAR(16)      NOT NULL DEFAULT 'pending' COMMENT '向量化状态：pending/processing/completed/failed',
    `vector_collection` VARCHAR(128)                           COMMENT 'Milvus Collection 名称',
    `description`     VARCHAR(512)                             COMMENT '知识库描述',
    `upload_user_id`  BIGINT UNSIGNED                          COMMENT '上传用户ID',
    `error_msg`       TEXT                                     COMMENT '向量化失败原因',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_vector_status` (`vector_status`),
    KEY `idx_upload_user_id` (`upload_user_id`),
    KEY `idx_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档管理表';


-- ============================================================
-- 6. 流程编排图表 workflow_graph
--    存储 SpringAIAlibabaGraph 可视化流程编排定义
-- ============================================================
CREATE TABLE IF NOT EXISTS `workflow_graph` (
    `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `graph_name`      VARCHAR(128)     NOT NULL                COMMENT '流程图名称',
    `description`     VARCHAR(512)                             COMMENT '流程描述',
    `graph_json`      LONGTEXT         NOT NULL                COMMENT '流程图 JSON 定义（节点+边+配置）',
    `version`         INT              NOT NULL DEFAULT 1      COMMENT '版本号',
    `enabled`         TINYINT(1)       NOT NULL DEFAULT 1      COMMENT '是否启用',
    `graph_type`      VARCHAR(32)      NOT NULL DEFAULT 'customer_service' COMMENT '图类型：customer_service/assistant',
    `created_by`      BIGINT UNSIGNED                          COMMENT '创建用户ID',
    `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_graph_type` (`graph_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程编排图配置表';

-- 初始化默认客服流程图
INSERT INTO `workflow_graph` (`graph_name`, `description`, `graph_json`, `version`, `enabled`, `graph_type`)
VALUES (
    '默认客服流程',
    '包含意图识别、知识检索、业务执行、回复生成四个节点的标准客服流程',
    '{"nodes":[{"id":"intent","type":"intent_recognition","label":"意图识别","x":100,"y":200},{"id":"knowledge","type":"knowledge_retrieval","label":"知识检索","x":300,"y":100},{"id":"business","type":"business_execution","label":"业务执行","x":300,"y":300},{"id":"reply","type":"reply_generation","label":"回复生成","x":500,"y":200}],"edges":[{"source":"intent","target":"knowledge","condition":"knowledge"},{"source":"intent","target":"business","condition":"business"},{"source":"knowledge","target":"reply"},{"source":"business","target":"reply"}]}',
    1,
    1,
    'customer_service'
);


-- ============================================================
-- 索引补充说明
-- ============================================================
-- chat_message.memory_id：高频查询字段，按 memoryId 获取历史消息
-- chat_message.session_id：AI 助理会话关联查询
-- knowledge_base.vector_status：异步任务监控，按状态过滤
-- ai_model.enabled：前端获取可用模型列表时过滤

COMMIT;
