import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'CustomerService',
    component: () => import('@/views/CustomerServiceView.vue'),
    meta: { title: 'AI 智能客服', requiresAuth: false },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/assistant',
    name: 'Assistant',
    component: () => import('@/views/AssistantView.vue'),
    meta: { title: 'AI 助理', requiresAuth: true },
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/admin/models',
    children: [
      {
        path: 'models',
        name: 'ModelManage',
        component: () => import('@/views/admin/ModelManageView.vue'),
        meta: { title: '模型管理', requiresAuth: true, permission: 'permModel' },
      },
      {
        path: 'knowledge',
        name: 'KnowledgeManage',
        component: () => import('@/views/admin/KnowledgeManageView.vue'),
        meta: { title: '知识库管理', requiresAuth: true, permission: 'permKnowledge' },
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: { title: '用户管理', requiresAuth: true, permission: 'permUser' },
      },
      {
        path: 'graph',
        name: 'GraphEditor',
        component: () => import('@/views/admin/GraphEditorView.vue'),
        meta: { title: '流程编排', requiresAuth: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach(async (to) => {
  document.title = `${to.meta.title ?? 'AI 智能客服'} - AI系统`

  if (!to.meta.requiresAuth) return true

  const auth = useAuthStore()
  if (!auth.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  // 权限检查
  const perm = to.meta.permission as string | undefined
  if (perm && !auth.userInfo[perm]) {
    ElMessage.warning('暂无权限访问该页面')
    return { path: '/assistant' }
  }

  return true
})

export default router
