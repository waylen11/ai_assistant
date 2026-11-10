import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

// 后端 API 地址（从环境变量读取）
// 开发环境：使用绝对地址，如 http://localhost:8080
// 生产环境：使用空字符串，通过 Nginx 反向代理
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

// 完整 baseURL
const baseURL = apiBaseUrl ? `${apiBaseUrl}/api` : '/api'

const request = axios.create({
  baseURL,
  timeout: 30000,
})

// 请求拦截器：附加 Token
request.interceptors.request.use(
  (config) => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers['Authorization'] = auth.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      useAuthStore().logout()
      window.location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
