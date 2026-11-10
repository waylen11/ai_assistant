import request from '@/utils/request'
import CryptoJS from 'crypto-js'

/**
 * 获取用户盐值
 */
export const getSalt = (username: string) =>
  request.get(`/auth/salt/${username}`)

/**
 * 登录（密码会在前端进行哈希处理）
 */
export const login = async (data: { username: string; password: string }) => {
  // 1. 先获取盐值
  const saltRes = await getSalt(data.username)
  const salt = saltRes.data.salt

  // 2. 对密码进行哈希：SHA256(salt + password)
  const hashedPassword = CryptoJS.SHA256(salt + data.password).toString()

  // 3. 发送哈希后的密码
  return request.post('/auth/login', {
    username: data.username,
    password: hashedPassword
  })
}

export const logout = () => request.post('/auth/logout')

export const getUserInfo = () => request.get('/auth/info')
