<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-orb orb1" />
    <div class="bg-orb orb2" />
    <div class="bg-orb orb3" />

    <div class="login-card glass-card">
      <!-- Logo -->
      <div class="login-logo">
        <div class="logo-icon">
          <el-icon size="32"><Cpu /></el-icon>
        </div>
        <h1 class="logo-title">AI 智能助手</h1>
        <p class="logo-sub">登录您的账号以使用完整功能</p>
      </div>

      <!-- 表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
            autocomplete="current-password"
            @keydown.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          class="login-button"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form>

      <!-- 返回客服 -->
      <div class="back-link">
        <el-button link @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon>
          返回 AI 客服
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { login } from '@/api/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!await formRef.value?.validate().catch(() => false)) return

  loading.value = true
  try {
    const res = await login(form) as any
    authStore.setToken(res.data.token)
    authStore.setUserInfo(res.data)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/assistant'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

// 背景光晕
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  
  &.orb1 {
    width: 400px;
    height: 400px;
    background: rgba(99, 102, 241, 0.15);
    top: -100px;
    left: -100px;
    animation: drift 12s ease-in-out infinite;
  }
  
  &.orb2 {
    width: 300px;
    height: 300px;
    background: rgba(139, 92, 246, 0.12);
    bottom: -50px;
    right: -50px;
    animation: drift 16s ease-in-out infinite reverse;
  }
  
  &.orb3 {
    width: 200px;
    height: 200px;
    background: rgba(59, 130, 246, 0.1);
    top: 50%;
    left: 60%;
    animation: drift 10s ease-in-out infinite 3s;
  }
}

@keyframes drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 30px) scale(0.95); }
}

.login-card {
  width: 400px;
  padding: 48px 40px;
  position: relative;
  z-index: 10;
  
  @media (max-width: 480px) {
    width: calc(100vw - 32px);
    padding: 36px 24px;
  }
  
  transition: transform 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
  }
}

.login-logo {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin: 0 auto 16px;
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.4);
  animation: float 4s ease-in-out infinite;
}

.logo-title {
  font-size: 22px;
  font-weight: 700;
  color: #F1F5F9;
  margin-bottom: 6px;
}

.logo-sub {
  font-size: 13px;
  color: #64748B;
}

.login-form {
  .el-form-item {
    margin-bottom: 16px;
  }
}

.login-button {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #6366F1, #8B5CF6) !important;
  border: none !important;
  border-radius: 12px !important;
  margin-top: 8px;
  transition: all 0.3s ease;
  letter-spacing: 4px;
  
  &:hover {
    box-shadow: 0 6px 24px rgba(99, 102, 241, 0.45);
    transform: translateY(-1px);
  }
}

.back-link {
  text-align: center;
  margin-top: 20px;
  
  .el-button {
    color: #475569 !important;
    font-size: 13px;
    
    &:hover { color: #6366F1 !important; }
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
</style>
