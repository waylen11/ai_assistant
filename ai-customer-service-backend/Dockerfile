# =====================================================
# AI 智能客服 - 后端 Dockerfile（多阶段构建）
# Stage 1: Maven 构建
# Stage 2: JRE17 运行镜像（精简体积）
# =====================================================

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 先复制 pom.xml，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
# 预下载依赖（只要 pom.xml 不变，此层就走缓存）
RUN mvn dependency:go-offline -B \
    -s /usr/share/maven/conf/settings.xml \
    --no-transfer-progress

# 复制源码并构建（跳过测试加速打包）
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine AS runtime

# 添加非 root 用户，提升安全性
RUN addgroup -S aics && adduser -S aics -G aics

WORKDIR /app

# 从 builder 阶段复制 jar
COPY --from=builder /build/target/*.jar app.jar

# 设置文件所有权
RUN chown -R aics:aics /app

# 切换到非 root 用户
USER aics

# 暴露端口
EXPOSE 8080

# JVM 参数通过环境变量注入
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# 启动命令（支持外部传入 JAVA_OPTS）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
