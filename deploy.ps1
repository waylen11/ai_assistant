#!/usr/bin/env pwsh
# =====================================================
# AI 智能客服系统 - Windows PowerShell 一键部署脚本
# 适用于：Windows + Docker Desktop 环境
# =====================================================

param(
    [string]$Action = "start",   # start | stop | restart | logs | status | clean
    [string]$Service = ""        # 指定服务名（留空则操作全部）
)

$ProjectName = "aics"
$EnvFile = ".env"
$ComposeFile = "docker-compose.yml"

# ---- 颜色输出函数 ----
function Write-Green($msg) { Write-Host $msg -ForegroundColor Green }
function Write-Yellow($msg) { Write-Host $msg -ForegroundColor Yellow }
function Write-Red($msg) { Write-Host $msg -ForegroundColor Red }
function Write-Cyan($msg) { Write-Host $msg -ForegroundColor Cyan }

# ---- 切换到脚本所在目录 ----
Set-Location $PSScriptRoot

Write-Cyan "=============================================="
Write-Cyan "  AI 智能客服系统 - 部署管理脚本"
Write-Cyan "=============================================="
Write-Host ""

# ---- 检查 Docker ----
function Check-Docker {
    try {
        $null = docker info 2>&1
        if ($LASTEXITCODE -ne 0) { throw }
        Write-Green "✅ Docker 已就绪"
    } catch {
        Write-Red "❌ Docker 未运行，请先启动 Docker Desktop"
        exit 1
    }
}

# ---- 检查 .env 文件 ----
function Check-Env {
    if (-not (Test-Path $EnvFile)) {
        Write-Yellow "⚠️  未找到 .env 文件，正在从 .env.example 复制..."
        Copy-Item ".env.example" ".env"
        Write-Red "❗ 请先编辑 .env 文件，填写 DASHSCOPE_API_KEY 等必填项后重新运行！"
        Write-Yellow "   文件路径：$(Resolve-Path .env)"
        exit 1
    }
    # 检查 DASHSCOPE_API_KEY 是否已填写
    $envContent = Get-Content $EnvFile -Raw
    if ($envContent -match "DASHSCOPE_API_KEY=your-dashscope-api-key-here") {
        Write-Red "❗ 请先在 .env 文件中填写真实的 DASHSCOPE_API_KEY！"
        exit 1
    }
    Write-Green "✅ .env 配置已就绪"
}

# ---- 创建 MinIO 桶（首次运行需要）----
function Init-Minio {
    Write-Yellow "⏳ 等待 MinIO 启动并初始化存储桶..."
    Start-Sleep -Seconds 15
    
    # 使用 mc 命令创建桶（容器内执行）
    docker exec aics-minio sh -c `
        "mc alias set local http://localhost:9000 minioadmin minioadmin 2>/dev/null; `
         mc mb --ignore-existing local/knowledge-base; `
         mc mb --ignore-existing local/user-avatars; `
         mc anonymous set public local/user-avatars" 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Green "✅ MinIO 桶初始化完成（knowledge-base / user-avatars）"
    } else {
        Write-Yellow "⚠️  MinIO 桶初始化跳过（可能已存在）"
    }
}

# ---- 主流程 ----
switch ($Action.ToLower()) {

    "start" {
        Check-Docker
        Check-Env
        
        Write-Cyan "🚀 启动全栈服务..."
        Write-Host ""
        
        # 先启动基础中间件
        Write-Yellow "📦 [1/3] 启动基础中间件（MySQL / Redis / MinIO / Milvus）..."
        docker-compose --project-name $ProjectName up -d mysql redis minio etcd milvus
        
        Write-Yellow "⏳ 等待中间件就绪（约 30 秒）..."
        Start-Sleep -Seconds 30
        
        # 初始化 MinIO 桶
        Init-Minio
        
        # 启动后端
        Write-Yellow "⚙️  [2/3] 构建并启动后端服务（首次构建需要约 3-5 分钟）..."
        docker-compose --project-name $ProjectName up -d --build backend
        
        Write-Yellow "⏳ 等待后端就绪（约 30 秒）..."
        Start-Sleep -Seconds 30
        
        # 启动前端
        Write-Yellow "🎨 [3/3] 构建并启动前端服务..."
        docker-compose --project-name $ProjectName up -d --build frontend
        
        Write-Host ""
        Write-Green "=============================================="
        Write-Green "  ✅ 部署成功！"
        Write-Green "=============================================="
        Write-Host ""
        Write-Cyan "📌 访问地址："
        Write-Host "   🌐 应用主页：      http://localhost"
        Write-Host "   🔧 后端 API：      http://localhost:8080"
        Write-Host "   🗄️  MinIO 控制台：  http://localhost:9001  (admin/minioadmin)"
        Write-Host "   📊 Milvus：        http://localhost:19530"
        Write-Host ""
        Write-Cyan "📌 默认账号："
        Write-Host "   👤 管理员：        admin / admin123"
        Write-Host ""
    }

    "stop" {
        Check-Docker
        Write-Yellow "⏹️  停止所有服务..."
        docker-compose --project-name $ProjectName stop
        Write-Green "✅ 所有服务已停止（数据已保留）"
    }

    "restart" {
        Check-Docker
        if ($Service) {
            Write-Yellow "🔄 重启服务：$Service"
            docker-compose --project-name $ProjectName restart $Service
        } else {
            Write-Yellow "🔄 重启所有服务..."
            docker-compose --project-name $ProjectName restart
        }
        Write-Green "✅ 重启完成"
    }

    "logs" {
        Check-Docker
        if ($Service) {
            docker-compose --project-name $ProjectName logs -f --tail=100 $Service
        } else {
            docker-compose --project-name $ProjectName logs -f --tail=50
        }
    }

    "status" {
        Check-Docker
        Write-Cyan "📊 服务状态："
        docker-compose --project-name $ProjectName ps
    }

    "clean" {
        Check-Docker
        Write-Red "⚠️  警告：此操作将删除所有容器和数据卷！"
        $confirm = Read-Host "确认删除？请输入 YES 继续"
        if ($confirm -eq "YES") {
            docker-compose --project-name $ProjectName down -v --remove-orphans
            Write-Green "✅ 已清理所有容器和数据卷"
        } else {
            Write-Yellow "已取消"
        }
    }

    default {
        Write-Host "用法："
        Write-Host "  .\deploy.ps1 start              # 启动全部服务"
        Write-Host "  .\deploy.ps1 stop               # 停止全部服务"
        Write-Host "  .\deploy.ps1 restart            # 重启全部服务"
        Write-Host "  .\deploy.ps1 restart backend    # 重启后端"
        Write-Host "  .\deploy.ps1 logs               # 查看所有日志"
        Write-Host "  .\deploy.ps1 logs backend       # 查看后端日志"
        Write-Host "  .\deploy.ps1 status             # 查看服务状态"
        Write-Host "  .\deploy.ps1 clean              # 清理所有（含数据！）"
    }
}
