<template>
  <div class="customer-service-page">
    <!-- 粒子背景 -->
    <canvas ref="canvasRef" class="particles-canvas" />

    <!-- 顶部栏 -->
    <header class="top-bar glass-card">
      <div class="top-bar-left">
        <div class="logo">
          <div class="logo-icon">
            <el-icon size="22"><Cpu /></el-icon>
          </div>
          <div class="logo-text">
            <span class="logo-title">AI 智能客服</span>
            <span class="logo-sub">在线 · 随时为您服务</span>
          </div>
        </div>
      </div>
      <div class="top-bar-center">
        <ModelSelector />
      </div>
      <div class="top-bar-right">
        <el-button
          v-if="!authStore.isLoggedIn"
          size="small"
          class="login-btn"
          @click="$router.push('/login')"
        >
          <el-icon><User /></el-icon> 登录系统
        </el-button>
        <template v-else>
          <el-avatar :size="32" :src="authStore.userInfo.avatarUrl" class="user-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <el-dropdown @command="handleCommand">
            <span class="dropdown-trigger">
              {{ authStore.userInfo.nickname || authStore.userInfo.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="assistant">AI 助理</el-dropdown-item>
                <el-dropdown-item command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </div>
    </header>

    <!-- 聊天主区 -->
    <main class="chat-main">
      <div class="chat-container">
        <!-- 欢迎界面（无消息时） -->
        <div v-if="chatStore.messages.length === 0" class="welcome-area">
          <div class="welcome-icon">
            <el-icon size="64"><Cpu /></el-icon>
          </div>
          <h2 class="welcome-title">您好，有什么可以帮到您？</h2>
          <p class="welcome-sub">我可以为您提供产品咨询、在线预约、企业信息查询等服务</p>
          <div class="quick-actions">
            <button
              v-for="action in quickActions"
              :key="action"
              class="quick-action-btn glass-card"
              @click="sendQuick(action)"
            >
              {{ action }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div ref="messageListRef" class="message-list">
          <ChatBubble
            v-for="msg in chatStore.messages"
            :key="msg.id"
            :message="msg"
          />
        </div>
      </div>
    </main>

    <!-- 输入区 -->
    <footer class="input-area">
      <div class="input-container glass-card">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="1"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入您的问题，按 Enter 发送，Shift+Enter 换行..."
          class="chat-input"
          :disabled="chatStore.isStreaming"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="input-actions">
          <!-- 停止按钮 -->
          <el-button
            v-if="chatStore.isStreaming"
            circle
            class="stop-btn"
            @click="stopChat"
          >
            <el-icon><VideoPause /></el-icon>
          </el-button>
          <!-- 发送按钮 -->
          <el-button
            v-else
            circle
            class="send-btn"
            :disabled="!inputText.trim()"
            @click="sendMessage"
          >
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
      <p class="input-hint">AI 生成内容仅供参考，请注意核实重要信息</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useModelStore } from '@/stores/model'
import { streamRequest } from '@/utils/sse'
import ChatBubble from '@/components/ChatBubble.vue'
import ModelSelector from '@/components/ModelSelector.vue'
import { logout as logoutApi } from '@/api/auth'

const router = useRouter()
const chatStore = useChatStore()
const authStore = useAuthStore()
const modelStore = useModelStore()

const inputText = ref('')
const messageListRef = ref<HTMLElement>()
const canvasRef = ref<HTMLCanvasElement>()

const quickActions = [
  '公司有哪些产品？',
  '如何在线预约服务？',
  '公司地址和联系方式',
  '售后服务政策',
]

onMounted(() => {
  initParticles()
})

// 自动滚动到底部
watch(
  () => chatStore.messages.length,
  () => nextTick(() => scrollToBottom())
)

// 监听最后一条消息内容变化（流式更新时）
watch(
  () => chatStore.messages[chatStore.messages.length - 1]?.content,
  () => nextTick(() => scrollToBottom())
)

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || chatStore.isStreaming) return

  inputText.value = ''

  // 添加用户消息
  chatStore.addMessage({
    id: Date.now().toString(),
    role: 'user',
    content: text,
    timestamp: Date.now(),
  })

  // 添加 AI 消息占位
  const aiMsgId = (Date.now() + 1).toString()
  chatStore.addMessage({
    id: aiMsgId,
    role: 'assistant',
    content: '',
    streaming: true,
    timestamp: Date.now(),
  })

  chatStore.isStreaming = true
  const controller = new AbortController()
  chatStore.abortController = controller

  const modelName = modelStore.currentModel?.modelName || ''

  await streamRequest({
    url: '/api/chat/stream',
    params: { message: text, modelName },
    signal: controller.signal,
    onChunk: (chunk) => {
      chatStore.updateLastAssistantMessage(chunk)
    },
    onDone: () => {
      chatStore.isStreaming = false
      const last = chatStore.messages[chatStore.messages.length - 1]
      if (last) last.streaming = false
      chatStore.abortController = null
    },
    onError: (err) => {
      chatStore.isStreaming = false
      const last = chatStore.messages[chatStore.messages.length - 1]
      if (last) {
        last.streaming = false
        if (!last.content) last.content = '抱歉，发生了一些错误，请稍后重试。'
      }
      chatStore.abortController = null
    },
  })
}

function sendQuick(text: string) {
  inputText.value = text
  sendMessage()
}

function stopChat() {
  chatStore.stopStreaming()
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await logoutApi()
    authStore.logout()
    router.push('/')
  } else if (cmd === 'assistant') {
    router.push('/assistant')
  } else if (cmd === 'admin') {
    router.push('/admin/models')
  }
}

// 粒子背景动画
function initParticles() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')!

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles: { x: number; y: number; vx: number; vy: number; size: number; alpha: number }[] = []
  const count = 80

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      size: Math.random() * 2 + 0.5,
      alpha: Math.random() * 0.5 + 0.1,
    })
  }

  function animate() {
    ctx.clearRect(0, 0, canvas!.width, canvas!.height)
    particles.forEach(p => {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > canvas!.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas!.height) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(99, 102, 241, ${p.alpha})`
      ctx.fill()
    })
    requestAnimationFrame(animate)
  }
  animate()

  window.addEventListener('resize', () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  })
}
</script>

<style lang="scss" scoped>
.customer-service-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.particles-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

// 顶部栏
.top-bar {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  margin: 12px 16px 0;
  border-radius: 16px;
  flex-shrink: 0;
  
  @media (max-width: 768px) {
    padding: 10px 16px;
    margin: 8px 8px 0;
  }
}

.top-bar-left, .top-bar-right, .top-bar-center {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.logo-title {
  font-size: 16px;
  font-weight: 600;
  color: #F1F5F9;
  display: block;
}

.logo-sub {
  font-size: 11px;
  color: #10B981;
  display: block;
}

.login-btn {
  background: rgba(99, 102, 241, 0.15) !important;
  border: 1px solid rgba(99, 102, 241, 0.3) !important;
  color: #818CF8 !important;
  border-radius: 20px !important;
  
  &:hover {
    background: rgba(99, 102, 241, 0.25) !important;
  }
}

.user-avatar {
  cursor: pointer;
  border: 2px solid rgba(99, 102, 241, 0.4);
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94A3B8;
  font-size: 13px;
  cursor: pointer;
  
  &:hover { color: #F1F5F9; }
}

// 聊天主区
.chat-main {
  flex: 1;
  overflow: hidden;
  position: relative;
  z-index: 5;
  padding: 16px;
  
  @media (max-width: 768px) {
    padding: 8px;
  }
}

.chat-container {
  max-width: 800px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  flex-direction: column;
}

// 欢迎区
.welcome-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
}

.welcome-icon {
  width: 96px;
  height: 96px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.3);
  animation: float 4s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: #F1F5F9;
  margin-bottom: 8px;
}

.welcome-sub {
  font-size: 14px;
  color: #64748B;
  margin-bottom: 32px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.quick-action-btn {
  padding: 10px 20px;
  border: none;
  color: #94A3B8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  
  &:hover {
    color: #6366F1;
    border-color: rgba(99, 102, 241, 0.4) !important;
    background: rgba(99, 102, 241, 0.08) !important;
  }
}

// 消息列表
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  scroll-behavior: smooth;
}

// 输入区
.input-area {
  position: relative;
  z-index: 10;
  padding: 12px 16px 16px;
  flex-shrink: 0;
  
  @media (max-width: 768px) {
    padding: 8px 8px 12px;
  }
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 14px;
}

.chat-input {
  flex: 1;
  
  :deep(.el-textarea__inner) {
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;
    color: #F1F5F9 !important;
    resize: none;
    padding: 4px 0;
    font-size: 14px;
    line-height: 1.6;
    
    &::placeholder { color: #475569; }
  }
}

.send-btn {
  background: linear-gradient(135deg, #6366F1, #8B5CF6) !important;
  border: none !important;
  color: white !important;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.35);
  transition: all 0.2s;
  
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 4px 16px rgba(99, 102, 241, 0.5);
  }
  
  &:disabled {
    opacity: 0.4;
    background: rgba(255, 255, 255, 0.1) !important;
    box-shadow: none;
    transform: none;
  }
}

.stop-btn {
  background: rgba(239, 68, 68, 0.15) !important;
  border: 1px solid rgba(239, 68, 68, 0.3) !important;
  color: #EF4444 !important;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  animation: pulse-red 1.5s ease-in-out infinite;
}

@keyframes pulse-red {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
  50% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0.1); }
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: #334155;
  margin-top: 6px;
}
</style>
