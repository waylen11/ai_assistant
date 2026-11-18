# AI 智能客服系统

基于 Spring Boot 3 + Spring AI Alibaba 的全栈 AI 客服项目。支持流式对话、多模型动态切换、RAG 知识库检索，后端为本人独立开发并维护（前端基于开源模板二次开发）。

## 功能

- **流式对话**：基于 SSE（Server-Sent Events）逐 token 输出，前端实时渲染 Markdown 表格与代码高亮
- **多模型动态切换**：模型配置存于 MySQL，管理后台增删改，无需重启。DashScope 原生协议与 OpenAI 兼容协议（DeepSeek 等）两条通道，由工厂类按 baseUrl 自动路由并缓存客户端
- **RAG 知识库**：上传文档 → MinIO 存储 → TokenTextSplitter 分块 → 向量化写入 Milvus → 对话时检索 topK 片段注入系统提示词
- **对话记忆**：Redis List 存储短期记忆（滑动窗口截断 + TTL 过期），MySQL 持久化全量消息；工作记忆喂给模型，全量记录供前端回显
- **登录鉴权**：Sa-Token + JWT，管理后台按模块分配权限（模型 / 知识库 / 用户）
- **流程编排**：基于 Spring AI Alibaba Graph，意图识别 → 知识检索 → 业务执行 → 回复生成（意图识别当前为关键词规则匹配）

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.2、Spring AI 1.1、Spring AI Alibaba 1.1.2、Sa-Token、Spring Data JPA |
| 前端 | Vue 3、TypeScript、Element Plus、Pinia、Vite |
| 存储 | MySQL 8、Redis、Milvus 2.4（向量检索）、MinIO（对象存储） |
| 部署 | Docker Compose、Nginx |

## 快速开始（Docker Compose）

前置条件：Docker Desktop、阿里云百炼 API Key（[申请地址](https://dashscope.console.aliyun.com/apiKey)）。

```bash
git clone https://github.com/waylen11/ai_assistant.git
cd ai_assistant

# 配置环境变量
cp .env.example .env
# 编辑 .env，填写必填项：
#   DASHSCOPE_API_KEY=你的API Key
#   JWT_SECRET_KEY=32位以上随机字符串

docker compose up -d --build   # 首次构建约 5~10 分钟
```

启动后访问 `http://localhost`，默认账号 `admin / admin123`。

> 注意：MySQL 容器映射宿主机 3306 端口。若本机 3306 已被占用（如 Windows 的 MySQL80 服务），将 `docker-compose.yml` 中端口映射改为 `3307:3306`，并同步修改数据源 URL 的端口。

## 本地开发

前置条件：Java 17+、Maven 3.6+、Node.js 14+，以及本地 MySQL / Redis。

```bash
# 1. 初始化数据库（库名 ai_customer_service）
mysql -u root -p < ai-customer-service-backend/src/main/resources/db/schema.sql

# 2. 创建本地配置文件（该文件被 .gitignore 忽略，可放心填写真实密码）
#    位置：ai-customer-service-backend/src/main/resources/config.yml
#    application.yml 通过 spring.config.import 强制加载此文件，缺失时后端无法启动
```

`config.yml` 模板（按实际环境填写）：

```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/ai_customer_service?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME: root
SPRING_DATASOURCE_PASSWORD: "你的MySQL密码"

SPRING_DATA_REDIS_HOST: localhost
SPRING_DATA_REDIS_PORT: 6379
SPRING_DATA_REDIS_PASSWORD:

SPRING_AI_DASHSCOPE_API_KEY: "你的API Key"

SA_TOKEN_JWT_SECRET_KEY: 32位以上随机字符串
```

```bash
# 3. 启动后端
cd ai-customer-service-backend
mvn spring-boot:run            # 看到 Started AiCustomerServiceApplication 即成功

# 4. 启动前端
cd ai-customer-service-frontend
npm install
npm run dev                    # 访问 http://localhost:5173
```

## 添加模型

在管理后台「模型管理」中添加。`baseUrl` 为空走 DashScope 通道（API Key 来自服务端配置）；`baseUrl` 非空走 OpenAI 兼容通道（API Key 存于模型记录中）。

| 服务商 | baseUrl |
|---|---|
| 通义千问 | 留空 |
| DeepSeek | `https://api.deepseek.com` |
| OpenAI | `https://api.openai.com` |
| 本地 Ollama | `http://localhost:11434/v1` |

## 项目结构

```
ai-customer/
├── ai-customer-service-backend/        # Spring Boot 后端
│   └── src/main/java/com/huangyuanwei/aics/
│       ├── controller/                 # ChatController(SSE流式) / AssistantController /
│       │                               # KnowledgeController / ModelController /
│       │                               # AuthController / UserController / GraphController
│       ├── service/                    # ChatService(Redis记忆+流组装) / KnowledgeService(RAG)
│       ├── graph/                      # CustomerServiceGraph 流程编排
│       ├── config/                     # ChatClientFactory(多模型路由) / VectorStoreConfig /
│       │                               # SaTokenConfig
│       ├── entity/  repository/  dto/  enums/
│   └── src/main/resources/
│       ├── application.yml             # 主配置（引用 config.yml 覆盖敏感项）
│       └── db/schema.sql               # 建表与初始数据
├── ai-customer-service-frontend/       # Vue 3 前端
│   └── src/  (views / components / stores / api / utils)
├── docs/                               # 开发文档
├── docker-compose.yml
└── .env.example                        # Docker 部署环境变量模板
```

## 已知限制

- 意图识别为关键词规则匹配，非模型驱动，边界外的问题统一走知识检索分支
- 消息持久化与文档向量化使用 `@Async`，但为同类内部调用，代理未生效，实际同步执行
- 知识库删除仅删 MySQL 记录，Milvus 向量与 MinIO 原件未级联删除
- 流式响应的落库回调使用 `doOnComplete`，客户端中途断开时该轮消息不会保存
- 公开客服接口以客户端 IP 作为记忆标识，同一出口 IP 下多用户共享会话记忆
- Milvus 默认关闭（`spring.ai.vectorstore.milvus.enabled=false`），关闭时知识库可上传但语义检索不生效；开启需本地部署 Milvus 并配置 embedding 维度 1536

## 协议

MIT License
