<template>
  <div id="app-root">
    <!-- 动态星空背景 -->
    <div class="star-bg" />
    <!-- 路由视图 -->
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getUserInfo } from '@/api/auth'

const authStore = useAuthStore()

// 应用启动时，如已登录则静默刷新用户信息（确保权限数据最新）
onMounted(async () => {
  if (authStore.isLoggedIn) {
    try {
      const res = await getUserInfo() as any
      if (res?.data) authStore.setUserInfo(res.data)
    } catch {
      // 静默失败（token 过期等，路由守卫会处理跳转）
    }
  }
})
</script>

<style lang="scss">
#app-root {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>
