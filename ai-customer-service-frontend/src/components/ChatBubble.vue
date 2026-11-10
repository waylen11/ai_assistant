<template>
  <div class="chat-bubble fade-in" :class="message.role">
    <!-- AI 头像 -->
    <div v-if="message.role === 'assistant'" class="avatar ai-avatar">
      <el-icon><Cpu /></el-icon>
    </div>

    <div class="bubble-content">
      <!-- AI 消息：Markdown 渲染 -->
      <div
        v-if="message.role === 'assistant'"
        class="bubble ai-bubble"
        :class="{ typing: message.streaming }"
        v-html="renderedContent"
      />
      <!-- 用户消息 -->
      <div v-else class="bubble user-bubble">
        {{ message.content }}
      </div>
      <div class="bubble-time">{{ formatTime(message.timestamp) }}</div>
    </div>

    <!-- 用户头像 -->
    <div v-if="message.role === 'user'" class="avatar user-avatar">
      <el-icon><User /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import type { ChatMessageItem } from '@/stores/chat'

const props = defineProps<{ message: ChatMessageItem }>()

// ── 自定义渲染器：为 table 外包横向滚动容器 ──────────────────────────────
// marked v12 table 渲染器签名：table(header: string, body: string): string
const renderer = {
  table(header: string, body: string): string {
    return `<div class="table-scroll"><table><thead>${header}</thead><tbody>${body}</tbody></table></div>`
  }
}

// 模块级别只配置一次，避免重复注册
marked.use({
  gfm: true,       // 开启 GitHub Flavored Markdown（表格必须）
  breaks: false,   // 不强制换行，避免影响表格分隔行解析
  renderer
})

// ── 检测内容中是否含有表格语法 ─────────────────────────────────
function hasTableSyntax(content: string): boolean {
  // 检测标准Markdown表格：以 | 开头的行 + 含 --- 的分隔行
  const hasStandardTable = /^\|.+\|/m.test(content) && /^\|[\s\-:|]+\|/m.test(content)
  
  // 检测特殊格式表格：包含 | 分隔符的行
  const hasSpecialTable = /\|.*\|/m.test(content)
  
  // 检测自定义格式表格：包含 || 分隔符的行
  const hasCustomTable = /\|\|.*\|\|/m.test(content)
  
  return hasStandardTable || hasSpecialTable || hasCustomTable
}

// ── 转换特殊格式表格为标准Markdown表格 ─────────────────────────────────
function convertToMarkdownTable(content: string): string {
  // 检查是否包含表格相关内容
  if (!/\|.*\|/m.test(content) && !/\|\|.*\|\|/m.test(content)) {
    return content
  }
  
  // 按行分割内容
  const lines = content.split('\n')
  let inTable = false
  let tableLines: string[] = []
  let resultLines: string[] = []
  
  for (const line of lines) {
    // 检查是否是表格行（包含 | 或 || 分隔符）
    if (line.includes('|')) {
      if (!inTable) {
        // 开始表格
        inTable = true
        tableLines = [line]
      } else {
        // 继续表格
        tableLines.push(line)
      }
    } else {
      if (inTable) {
        // 结束表格，处理表格内容
        if (tableLines.length >= 2) {
          // 处理表格内容
          const processedTable = processTable(tableLines)
          resultLines.push(...processedTable)
        } else {
          // 表格行数不足，直接添加
          resultLines.push(...tableLines)
        }
        
        inTable = false
        tableLines = []
      }
      
      // 添加非表格行
      resultLines.push(line)
    }
  }
  
  // 处理最后一个表格
  if (inTable && tableLines.length >= 2) {
    const processedTable = processTable(tableLines)
    resultLines.push(...processedTable)
  } else if (inTable) {
    resultLines.push(...tableLines)
  }
  
  return resultLines.join('\n')
}

// ── 处理表格内容 ──────────────────────────────────────────────────────────
function processTable(tableLines: string[]): string[] {
  // 清理表格行，处理特殊格式
  const cleanedLines = tableLines.map(line => {
    // 处理 || 格式的表格
    if (line.includes('||')) {
      return line.replace(/\|\|/g, '|')
    }
    // 处理其他格式
    return line
  })
  
  // 确保表格有表头和分隔行
  if (cleanedLines.length >= 1) {
    const header = cleanedLines[0]
    const columns = header.split('|').filter(c => c.trim() !== '').length
    
    // 检查是否已有分隔行
    const hasSeparator = cleanedLines.some(line => /^\|[\s\-:|]+\|$/.test(line))
    
    if (!hasSeparator) {
      // 添加分隔行
      const separator = '|' + Array(columns).fill('---').join('|') + '|'
      cleanedLines.splice(1, 0, separator)
    }
  }
  
  return cleanedLines
}

// ── 原始文本安全转义 ──────────────────────────────────────────────────────
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

// ── 响应式渲染 ────────────────────────────────────────────────────────────
const renderedContent = computed(() => {
  let content = props.message.content || ''

  // 流式中且检测到表格语法：整体退回原始文本展示，避免乱码
  if (props.message.streaming && hasTableSyntax(content)) {
    return `<pre class="streaming-raw">${escapeHtml(content)}</pre>`
  }

  // 正常情况：先转换特殊格式表格，再处理 <br> 标签，最后进行 Markdown 解析
  content = convertToMarkdownTable(content)
  // 处理 <br> 标签，替换为 Markdown 换行格式（支持多种变体）
  content = content.replace(/<br\s*\/?>/gi, '  \n')
  return marked.parse(content) as string
})

function formatTime(ts: number) {
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style lang="scss" scoped>
.chat-bubble {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
  
  &.user {
    flex-direction: row-reverse;
  }
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
  
  &.ai-avatar {
    background: linear-gradient(135deg, #6366F1, #8B5CF6);
    color: white;
    box-shadow: 0 2px 10px rgba(99, 102, 241, 0.4);
  }
  
  &.user-avatar {
    background: linear-gradient(135deg, #1A1A2E, #16213E);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: #94A3B8;
  }
}

.bubble-content {
  max-width: 70%;
  min-width: 0; /* 防止内容撑破 flex 布局 */
  
  @media (max-width: 768px) {
    max-width: 85%;
  }
}

.bubble {
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-word;
  
  &.ai-bubble {
    background: rgba(255, 255, 255, 0.08);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 4px 16px 16px 16px;
    color: #F1F5F9;
    
    &.typing::after {
      content: '▍';
      animation: blink 0.7s step-end infinite;
      color: #6366F1;
      margin-left: 2px;
    }
    
    // ── Markdown 基础样式 ────────────────────────────────────────────────
    :deep(p) { margin-bottom: 8px; &:last-child { margin-bottom: 0; } }

    :deep(code) {
      background: rgba(99, 102, 241, 0.15);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
      color: #818CF8;
    }

    :deep(pre) {
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 8px;
      padding: 12px;
      overflow-x: auto;
      margin: 8px 0;
      
      code { background: transparent; padding: 0; color: #E2E8F0; }
    }

    :deep(ul), :deep(ol) { padding-left: 20px; margin: 8px 0; }
    :deep(li) { margin-bottom: 6px; line-height: 1.5; }
    :deep(ul ul), :deep(ol ul), :deep(ul ol), :deep(ol ol) { padding-left: 16px; margin: 4px 0; }
    :deep(ul ul li), :deep(ol ul li), :deep(ul ol li), :deep(ol ol li) { margin-bottom: 2px; }
    :deep(strong) { color: #818CF8; }
    :deep(h1, h2, h3) { color: #F1F5F9; margin: 12px 0 6px; }
    :deep(blockquote) {
      border-left: 3px solid #6366F1;
      margin: 8px 0;
      padding: 4px 12px;
      color: #94A3B8;
    }

    // ── 表格横向滚动容器 ─────────────────────────────────────────────────
    :deep(.table-scroll) {
      overflow-x: auto;
      margin: 10px 0;
      border-radius: 8px;
      // 自定义滚动条（深色主题）
      &::-webkit-scrollbar {
        height: 4px;
      }
      &::-webkit-scrollbar-track {
        background: rgba(255, 255, 255, 0.04);
        border-radius: 2px;
      }
      &::-webkit-scrollbar-thumb {
        background: rgba(99, 102, 241, 0.4);
        border-radius: 2px;
      }
    }

    // ── 表格样式 ─────────────────────────────────────────────────────────
    :deep(table) {
      border-collapse: collapse;
      min-width: 100%;   /* 内容够宽时撑满，不够宽时由容器滚动 */
      width: max-content;
      margin: 0;
      font-size: 13px;
    }

    :deep(thead) {
      position: sticky;
      top: 0;
    }

    :deep(th) {
      background: rgba(99, 102, 241, 0.2);
      border: 1px solid rgba(99, 102, 241, 0.25);
      padding: 8px 14px;
      text-align: left;
      color: #C4B5FD;
      font-weight: 600;
      white-space: nowrap;
    }

    :deep(td) {
      border: 1px solid rgba(255, 255, 255, 0.08);
      padding: 7px 14px;
      text-align: left;
      color: #E2E8F0;
    }

    :deep(tr:nth-child(even) td) {
      background: rgba(255, 255, 255, 0.03);
    }

    :deep(tr:hover td) {
      background: rgba(99, 102, 241, 0.08);
      transition: background 0.15s ease;
    }

    // ── 流式原始文本样式 ──────────────────────────────────────────────────
    :deep(.streaming-raw) {
      font-family: inherit;
      font-size: 14px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-word;
      background: transparent;
      border: none;
      padding: 0;
      margin: 0;
      color: #F1F5F9;
      overflow-x: visible;
    }
  }
  
  &.user-bubble {
    background: linear-gradient(135deg, #6366F1, #8B5CF6);
    color: white;
    border-radius: 16px 4px 16px 16px;
    box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
  }
}

.bubble-time {
  font-size: 11px;
  color: #475569;
  margin-top: 4px;
  
  .user & { text-align: right; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
