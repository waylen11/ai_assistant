import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatarUrl: string
  permModel: boolean
  permKnowledge: boolean
  permUser: boolean
  [key: string]: any
}

// 从 localStorage 恢复 userInfo（防止刷新后丢失权限）
function loadUserInfo(): UserInfo {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) : ({} as UserInfo)
  } catch {
    return {} as UserInfo
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<UserInfo>(loadUserInfo())

  const isLoggedIn = computed(() => !!token.value)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    token.value = null
    userInfo.value = {} as UserInfo
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isLoggedIn, setToken, setUserInfo, logout }
})

