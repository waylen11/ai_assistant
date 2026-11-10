<template>
  <div class="admin-layout">
    <!-- 侧边导航 -->
    <aside class="admin-sidebar glass-card">
      <div class="admin-logo">
        <div class="logo-icon"><el-icon size="20"><Setting /></el-icon></div>
        <span>管理后台</span>
      </div>
      <nav class="admin-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          active-class="active"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="admin-nav-footer">
        <router-link to="/" class="nav-item">
          <el-icon><HomeFilled /></el-icon>
          <span>回到客服</span>
        </router-link>
      </div>
    </aside>
    <!-- 内容区 -->
    <main class="admin-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()

// 使用 computed 保持响应式，userInfo 更新后菜单自动刷新
const navItems = computed(() => [
  { path: '/admin/models',    label: '模型管理',   icon: 'Cpu',        show: auth.userInfo.permModel },
  { path: '/admin/knowledge', label: '知识库管理', icon: 'Files',      show: auth.userInfo.permKnowledge },
  { path: '/admin/users',     label: '用户管理',   icon: 'UserFilled', show: auth.userInfo.permUser },
  { path: '/admin/graph',     label: '流程编排',   icon: 'Share',      show: true },
].filter(item => item.show))
</script>

<style lang="scss" scoped>
.admin-layout {
  height: 100vh;
  display: flex;
  gap: 0;
}

.admin-sidebar {
  width: 220px;
  min-width: 220px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  margin: 12px 0 12px 12px;
  padding: 20px 12px;
  border-radius: 16px;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 8px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #F1F5F9;
}

.logo-icon {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.admin-nav { flex: 1; display: flex; flex-direction: column; gap: 4px; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  color: #64748B;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
  
  &:hover { background: rgba(255,255,255,0.06); color: #94A3B8; }
  
  &.active {
    background: rgba(99, 102, 241, 0.15);
    color: #818CF8;
    border: 1px solid rgba(99, 102, 241, 0.2);
  }
}

.admin-nav-footer {
  border-top: 1px solid rgba(255,255,255,0.06);
  padding-top: 12px;
}

.admin-content { flex: 1; overflow: hidden; padding: 12px 12px 12px 8px; }
</style>
