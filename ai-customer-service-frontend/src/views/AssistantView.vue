<template>
  <div class="assistant-page">
    <!-- 左侧会话栏 -->
    <aside class="sidebar glass-card">
      <div class="sidebar-header">
        <div class="sidebar-title">
          <el-icon><ChatDotRound /></el-icon>
          AI 助理
        </div>
        <el-button circle size="small" class="new-session-btn" @click="createNewSession">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <!-- 模型选择 -->
      <div class="sidebar-model">
        <ModelSelector />
      </div>

      <!-- 会话列表 -->
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: currentSessionId === session.sessionId }"
          @click="switchSession(session.sessionId)"
        >
          <el-icon class="session-icon"><ChatDotRound /></el-icon>
          <div class="session-info">
            <span class="session-title">{{ session.title }}</span>
            <span class="session-time">{{ formatDate(session.lastMessageAt) }}</span>
          </div>
          <el-button
            link
            size="small"
            class="delete-session-btn"
            @click.stop="deleteSession(session.sessionId)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>

        <div v-if="sessions.length === 0" class="no-sessions">
          <el-icon><ChatDotRound /></el-icon>
          <span>暂无会话，点击 + 新建</span>
        </div>
      </div>

      <!-- 用户信息 -->
      <div class="sidebar-footer">
        <el-avatar :size="32" :src="authStore.userInfo.avatarUrl">
          <el-icon><User /></el-icon>
        </el-avatar>
        <span class="username">{{ authStore.userInfo.nickname || authStore.userInfo.username }}</span>
        <el-dropdown @command="handleCommand">
          <el-button link><el-icon><MoreFilled /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="home">AI 客服首页</el-dropdown-item>
              <el-dropdown-item
                v-if="authStore.userInfo.permModel || authStore.userInfo.permKnowledge || authStore.userInfo.permUser"
                command="admin"
              >管理后台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <!-- 主聊天区 -->
    <main class="chat-main">
      <!-- 无会话提示 -->
      <div v-if="!currentSessionId" class="no-session-placeholder">
        <div class="placeholder-icon">
          <el-icon size="64"><ChatDotRound /></el-icon>
        </div>
        <h3>选择会话或新建会话</h3>
        <p>AI 助理可以帮您处理产品采购、入库、销售、出库等业务操作</p>
        <el-button class="create-btn" @click="createNewSession">
          <el-icon><Plus /></el-icon> 新建会话
        </el-button>
      </div>

      <template v-else>
        <!-- 消息列表 -->
        <div ref="messageListRef" class="message-list">
          <ChatBubble v-for="msg in chatStore.messages" :key="msg.id" :message="msg" />
        </div>

        <!-- 输入区 -->
        <div class="input-area">
          <div class="input-container glass-card">
            <el-input
              v-model="inputText"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 5 }"
              placeholder="输入业务指令，例如：帮我查询本月销售业绩..."
              class="chat-input"
              :disabled="chatStore.isStreaming"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="input-btns">
              <el-button v-if="chatStore.isStreaming" circle class="stop-btn" @click="chatStore.stopStreaming()">
                <el-icon><VideoPause /></el-icon>
              </el-button>
              <el-button v-else circle class="send-btn" :disabled="!inputText.trim()" @click="sendMessage">
                <el-icon><Promotion /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useModelStore } from '@/stores/model'
import { streamRequest } from '@/utils/sse'
import { getSessions, createSession as createSessionApi, deleteSession as deleteSessionApi } from '@/api/assistant'
import { logout as logoutApi } from '@/api/auth'
import ChatBubble from '@/components/ChatBubble.vue'
import ModelSelector from '@/components/ModelSelector.vue'

const router = useRouter()
const chatStore = useChatStore()
const authStore = useAuthStore()
const modelStore = useModelStore()

const sessions = ref<any[]>([])
const currentSessionId = ref('')
const inputText = ref('')
const messageListRef = ref<HTMLElement>()

onMounted(async () => {
  await fetchSessions()
})

watch(() => chatStore.messages[chatStore.messages.length - 1]?.content, () =>
  nextTick(() => {
    if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
)

async function fetchSessions() {
  const res = await getSessions() as any
  sessions.value = res.data || []
}

async function createNewSession() {
  const modelName = modelStore.currentModel?.modelName
  const res = await createSessionApi({ modelName }) as any
  const session = res.data
  sessions.value.unshift(session)
  await switchSession(session.sessionId)
}

function switchSession(sessionId: string) {
  currentSessionId.value = sessionId
  chatStore.clearMessages()
  // TODO: 可加载历史消息
}

async function deleteSession(sessionId: string) {
  await ElMessageBox.confirm('确认删除此会话？', '提示', { type: 'warning' })
  await deleteSessionApi(sessionId)
  sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
  if (currentSessionId.value === sessionId) {
    currentSessionId.value = ''
    chatStore.clearMessages()
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !currentSessionId.value || chatStore.isStreaming) return
  inputText.value = ''

  chatStore.addMessage({ id: Date.now().toString(), role: 'user', content: text, timestamp: Date.now() })
  chatStore.addMessage({ id: (Date.now() + 1).toString(), role: 'assistant', content: '', streaming: true, timestamp: Date.now() })
  chatStore.isStreaming = true

  const controller = new AbortController()
  chatStore.abortController = controller

  await streamRequest({
    url: '/api/assistant/stream',
    params: { sessionId: currentSessionId.value, message: text, modelName: modelStore.currentModel?.modelName || '' },
    signal: controller.signal,
    onChunk: (chunk) => chatStore.updateLastAssistantMessage(chunk),
    onDone: () => {
      chatStore.isStreaming = false
      const last = chatStore.messages[chatStore.messages.length - 1]
      if (last) last.streaming = false
      // 更新会话标题
      const session = sessions.value.find(s => s.sessionId === currentSessionId.value)
      if (session) session.lastMessageAt = new Date().toISOString()
    },
    onError: () => {
      chatStore.isStreaming = false
      const last = chatStore.messages[chatStore.messages.length - 1]
      if (last && !last.content) last.content = '抱歉，请求失败，请稍后重试。'
      if (last) last.streaming = false
    },
  })
}

function formatDate(date: string) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await logoutApi()
    authStore.logout()
    router.push('/')
  } else if (cmd === 'home') {
    router.push('/')
  } else if (cmd === 'admin') {
    router.push('/admin/models')
  }
}
</script>

<style lang="scss" scoped>
.assistant-page {
  height: 100vh;
  display: flex;
  gap: 0;
}

// 侧边栏
.sidebar {
  width: 260px;
  min-width: 260px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  margin: 12px 0 12px 12px;
  padding: 16px;
  border-radius: 16px;
  overflow: hidden;
  
  @media (max-width: 768px) {
    display: none;
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #F1F5F9;
  
  .el-icon { color: #6366F1; }
}

.new-session-btn {
  background: rgba(99, 102, 241, 0.15) !important;
  border: 1px solid rgba(99, 102, 241, 0.3) !important;
  color: #818CF8 !important;
}

.sidebar-model { margin-bottom: 12px; }

.session-list {
  flex: 1;
  overflow-y: auto;
  margin: 0 -8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  
  &:hover {
    background: rgba(255, 255, 255, 0.06);
    
    .delete-session-btn { opacity: 1; }
  }
  
  &.active {
    background: rgba(99, 102, 241, 0.15);
    border: 1px solid rgba(99, 102, 241, 0.2);
  }
}

.session-icon { color: #64748B; font-size: 14px; }

.session-info {
  flex: 1;
  overflow: hidden;
}

.session-title {
  display: block;
  font-size: 13px;
  color: #E2E8F0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  display: block;
  font-size: 11px;
  color: #475569;
}

.delete-session-btn {
  opacity: 0;
  color: #475569 !important;
  transition: opacity 0.2s;
  
  &:hover { color: #EF4444 !important; }
}

.no-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: #475569;
  font-size: 13px;
  
  .el-icon { font-size: 32px; }
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  
  .username {
    flex: 1;
    font-size: 13px;
    color: #94A3B8;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 主聊天区
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px;
}

.no-session-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 12px;
  
  .placeholder-icon {
    width: 96px;
    height: 96px;
    background: rgba(99, 102, 241, 0.1);
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #6366F1;
    margin-bottom: 8px;
  }
  
  h3 { font-size: 18px; color: #94A3B8; }
  p { font-size: 13px; color: #475569; max-width: 300px; }
}

.create-btn {
  background: rgba(99, 102, 241, 0.15) !important;
  border: 1px solid rgba(99, 102, 241, 0.3) !important;
  color: #818CF8 !important;
  border-radius: 20px !important;
  padding: 8px 20px !important;
  margin-top: 8px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  scroll-behavior: smooth;
}

.input-area {
  padding-top: 8px;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 14px;
}

.chat-input :deep(.el-textarea__inner) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  color: #F1F5F9 !important;
  resize: none;
  font-size: 14px;
  
  &::placeholder { color: #475569; }
}

.send-btn {
  background: linear-gradient(135deg, #6366F1, #8B5CF6) !important;
  border: none !important;
  color: white !important;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  
  &:disabled {
    opacity: 0.4;
    background: rgba(255, 255, 255, 0.1) !important;
  }
}

.stop-btn {
  background: rgba(239, 68, 68, 0.15) !important;
  border: 1px solid rgba(239, 68, 68, 0.3) !important;
  color: #EF4444 !important;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
}
</style>
