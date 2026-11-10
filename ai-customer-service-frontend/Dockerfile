# =====================================================
# AI 智能客服 - 前端 Dockerfile（多阶段构建）
# Stage 1: Node 构建打包
# Stage 2: Nginx 静态文件服务
# =====================================================

# ---- Stage 1: Build ----
FROM node:20-alpine AS builder

WORKDIR /app

# 定义构建参数（API 地址，默认走 Nginx 反向代理）
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# 先复制 package.json，利用层缓存
COPY package.json package-lock.json* ./

# 安装依赖（使用 npm ci 确保版本一致性）
RUN npm ci --prefer-offline --no-audit

# 复制源码并构建
COPY . .
RUN npm run build

# ---- Stage 2: Runtime (Nginx) ----
FROM nginx:1.25-alpine AS runtime

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# Nginx 前台运行
CMD ["nginx", "-g", "daemon off;"]
