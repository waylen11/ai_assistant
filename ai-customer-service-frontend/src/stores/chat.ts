import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ChatMessageItem {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  streaming?: boolean
  timestamp: number
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessageItem[]>([])
  const isStreaming = ref(false)
  const abortController = ref<AbortController | null>(null)

  function addMessage(msg: ChatMessageItem) {
    messages.value.push(msg)
  }

  function updateLastAssistantMessage(chunk: string) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content += chunk
    }
  }

  function stopStreaming() {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
    }
    isStreaming.value = false
    // 移除末尾的 streaming 标记
    const last = messages.value[messages.value.length - 1]
    if (last) last.streaming = false
  }

  function clearMessages() {
    messages.value = []
  }

  return { messages, isStreaming, abortController, addMessage, updateLastAssistantMessage, stopStreaming, clearMessages }
})
