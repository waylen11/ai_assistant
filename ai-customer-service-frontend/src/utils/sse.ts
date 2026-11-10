/**
 * SSE 流式请求工具
 * 使用 fetch + ReadableStream，支持 AbortController 取消
 */

export interface StreamOptions {
  url: string
  method?: 'GET' | 'POST'
  params?: Record<string, string>
  body?: object
  headers?: Record<string, string>
  onChunk: (chunk: string) => void
  onDone?: () => void
  onError?: (err: Error) => void
  signal?: AbortSignal
}

export async function streamRequest(options: StreamOptions) {
  const { url, method = 'GET', params, body, headers = {}, onChunk, onDone, onError, signal } = options

  // 后端 API 地址（从环境变量读取）
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

  // 拼接完整 URL
  // 开发环境：apiBaseUrl + url = http://localhost:8080/api/chat/stream
  // 生产环境：apiBaseUrl 为空，url = /api/chat/stream（经过 Nginx 代理）
  let finalUrl = apiBaseUrl ? `${apiBaseUrl}${url}` : url
  if (params) {
    const qs = new URLSearchParams(params).toString()
    finalUrl = `${finalUrl}?${qs}`
  }

  // 附加 Token
  const token = localStorage.getItem('token')
  if (token) {
    headers['Authorization'] = token
  }

  try {
    const response = await fetch(finalUrl, {
      method,
      headers: {
        'Accept': 'text/event-stream',
        'Content-Type': 'application/json',
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
      signal,
    })

    // 处理 HTTP 错误状态码
    if (!response.ok) {
      if (response.status === 401) {
        // 登录过期，清除 token 并跳转登录页
        localStorage.removeItem('token')
        window.location.href = '/login'
        throw new Error('登录已过期，请重新登录')
      } else if (response.status === 403) {
        throw new Error('没有权限执行此操作')
      } else if (response.status === 500) {
        throw new Error('服务器内部错误')
      } else {
        throw new Error(`请求失败: ${response.status}`)
      }
    }

    const reader = response.body?.getReader()
    if (!reader) throw new Error('No response body')

    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value, { stream: true })
      
      // 解析 SSE 格式（data: xxx\n\n）
      const lines = text.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data && data !== '[DONE]') {
            onChunk(data)
          }
        }
      }
    }

    onDone?.()
  } catch (err) {
    if ((err as Error).name === 'AbortError') {
      // 用户主动取消，不报错
      onDone?.()
    } else {
      onError?.(err as Error)
    }
  }
}
