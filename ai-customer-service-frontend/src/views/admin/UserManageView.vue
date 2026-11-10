<template>
  <div class="user-manage-page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-sub">管理系统用户及其模块访问权限</p>
      </div>
      <el-button class="add-btn" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新增用户
      </el-button>
    </div>

    <!-- 用户表格 -->
    <div class="table-wrapper glass-card">
      <el-table :data="userList" v-loading="loading" class="user-table">
        <!-- 用户信息 -->
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="row.avatarUrl" class="user-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-info">
                <span class="user-name">{{ row.nickname || row.username }}</span>
                <span class="user-username">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="部门 / 岗位" min-width="140">
          <template #default="{ row }">
            <div>
              <div class="cell-primary">{{ row.department || '-' }}</div>
              <div class="cell-sub">{{ row.position || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <!-- 权限标签 -->
        <el-table-column label="权限" width="220">
          <template #default="{ row }">
            <div class="perm-tags">
              <el-tag v-if="row.permModel" size="small" type="primary">模型管理</el-tag>
              <el-tag v-if="row.permKnowledge" size="small" type="success">知识库</el-tag>
              <el-tag v-if="row.permUser" size="small" type="warning">用户管理</el-tag>
              <span v-if="!row.permModel && !row.permKnowledge && !row.permUser" class="no-perm">无权限</span>
            </div>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              size="small"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="最后登录" width="140">
          <template #default="{ row }">
            <span class="cell-sub">{{ formatDate(row.lastLoginAt) }}</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="220" align="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button size="small" class="op-btn edit" @click="openDialog(row)">
                <el-icon><Edit /></el-icon> 编辑
              </el-button>
              <el-button size="small" class="op-btn perm" @click="openPermDialog(row)">
                <el-icon><Key /></el-icon> 权限
              </el-button>
              <el-dropdown @command="cmd => handleDropCmd(cmd, row)">
                <el-button size="small" class="op-btn more">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="resetPwd">重置密码</el-dropdown-item>
                    <el-dropdown-item command="delete" class="danger-item">删除用户</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editUser?.id ? '编辑用户' : '新增用户'"
      width="520px"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" :disabled="!!editUser?.id" placeholder="登录用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称">
              <el-input v-model="formData.nickname" placeholder="显示昵称" />
            </el-form-item>
          </el-col>
          <el-col v-if="!editUser?.id" :span="24">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="formData.password" type="password" show-password placeholder="初始密码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="formData.gender" style="width:100%">
                <el-option :value="0" label="未知" />
                <el-option :value="1" label="男" />
                <el-option :value="2" label="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="formData.department" placeholder="所属部门" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-input v-model="formData.position" placeholder="职位/岗位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="formData.email" placeholder="邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配弹窗 -->
    <el-dialog v-model="permDialogVisible" title="权限分配" width="400px">
      <div v-if="permUser" class="perm-dialog-content">
        <div class="perm-user-info">
          <el-avatar :size="42" :src="permUser.avatarUrl">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div>
            <p class="perm-username">{{ permUser.nickname || permUser.username }}</p>
            <p class="perm-dept">{{ permUser.department }} / {{ permUser.position }}</p>
          </div>
        </div>
        <div class="perm-divider" />
        <p class="perm-hint">勾选该用户可访问的管理模块：</p>
        <div class="perm-checkboxes">
          <div class="perm-item" :class="{ checked: permForm.permModel }" @click="permForm.permModel = !permForm.permModel">
            <div class="perm-item-icon model">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="perm-item-info">
              <span class="perm-item-label">模型管理</span>
              <span class="perm-item-sub">可查看/新增/编辑/删除 AI 模型配置</span>
            </div>
            <el-checkbox v-model="permForm.permModel" @click.stop />
          </div>
          <div class="perm-item" :class="{ checked: permForm.permKnowledge }" @click="permForm.permKnowledge = !permForm.permKnowledge">
            <div class="perm-item-icon knowledge">
              <el-icon><Files /></el-icon>
            </div>
            <div class="perm-item-info">
              <span class="perm-item-label">知识库管理</span>
              <span class="perm-item-sub">可上传/管理知识库文档，触发向量化</span>
            </div>
            <el-checkbox v-model="permForm.permKnowledge" @click.stop />
          </div>
          <div class="perm-item" :class="{ checked: permForm.permUser }" @click="permForm.permUser = !permForm.permUser">
            <div class="perm-item-icon user">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="perm-item-info">
              <span class="perm-item-label">用户管理</span>
              <span class="perm-item-sub">可新增/编辑/删除用户，分配权限</span>
            </div>
            <el-checkbox v-model="permForm.permUser" @click.stop />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPerm" @click="savePermissions">保存权限</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="360px">
      <el-form :model="resetPwdForm" ref="resetPwdRef" label-width="90px">
        <el-form-item label="新密码" prop="newPassword"
          :rules="[{ required: true, message: '请输入新密码' }, { min: 6, message: '密码不少于6位' }]">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPwd" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { getUserList, createUser, updateUser, deleteUser, toggleUser, resetPassword, assignPermissions } from '@/api/user'
import type { FormInstance, FormRules } from 'element-plus'

const userList = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const savingPerm = ref(false)
const savingPwd = ref(false)

const dialogVisible = ref(false)
const permDialogVisible = ref(false)
const resetPwdVisible = ref(false)

const editUser = ref<any>(null)
const permUser = ref<any>(null)
const resetPwdUser = ref<any>(null)

const formRef = ref<FormInstance>()
const resetPwdRef = ref<FormInstance>()

const formData = reactive({ username: '', nickname: '', password: '', gender: 0, department: '', position: '', email: '' })
const permForm = reactive({ permModel: false, permKnowledge: false, permUser: false })
const resetPwdForm = reactive({ newPassword: '' })

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }, { min: 6, message: '密码不少于6位' }],
}

onMounted(fetchList)

async function fetchList() {
  loading.value = true
  try {
    const res = await getUserList() as any
    userList.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(user?: any) {
  editUser.value = user || null
  if (user) {
    Object.assign(formData, { ...user, password: '' })
  } else {
    Object.assign(formData, { username: '', nickname: '', password: '', gender: 0, department: '', position: '', email: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    if (editUser.value?.id) {
      await updateUser(editUser.value.id, formData)
      ElMessage.success('用户信息已更新')
    } else {
      await createUser(formData)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

function openPermDialog(user: any) {
  permUser.value = user
  Object.assign(permForm, { permModel: user.permModel, permKnowledge: user.permKnowledge, permUser: user.permUser })
  permDialogVisible.value = true
}

async function savePermissions() {
  savingPerm.value = true
  try {
    await assignPermissions(permUser.value.id, { ...permForm })
    ElMessage.success('权限已更新')
    permDialogVisible.value = false
    fetchList()
  } finally {
    savingPerm.value = false
  }
}

async function handleToggle(user: any) {
  await toggleUser(user.id)
  user.enabled = !user.enabled
  ElMessage.success(user.enabled ? '已启用' : '已停用')
}

async function handleDropCmd(cmd: string, user: any) {
  if (cmd === 'delete') {
    await ElMessageBox.confirm(`确认删除用户 "${user.username}"？`, '提示', { type: 'warning' })
    await deleteUser(user.id)
    ElMessage.success('已删除')
    fetchList()
  } else if (cmd === 'resetPwd') {
    resetPwdUser.value = user
    resetPwdForm.newPassword = ''
    resetPwdVisible.value = true
  }
}

async function handleResetPwd() {
  if (!await resetPwdRef.value?.validate().catch(() => false)) return
  savingPwd.value = true
  try {
    await resetPassword(resetPwdUser.value.id, resetPwdForm.newPassword)
    ElMessage.success('密码已重置')
    resetPwdVisible.value = false
  } finally {
    savingPwd.value = false
  }
}

function formatDate(d: string) {
  if (!d) return '从未登录'
  return new Date(d).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style lang="scss" scoped>
.user-manage-page {
  height: 100%;
  overflow-y: auto;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.page-title { font-size: 20px; font-weight: 600; color: #F1F5F9; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: #64748B; }

.add-btn {
  background: linear-gradient(135deg, #6366F1, #8B5CF6) !important;
  border: none !important;
  color: white !important;
  border-radius: 10px !important;
}

.table-wrapper {
  padding: 0;
  overflow: hidden;
}

.user-cell { display: flex; align-items: center; gap: 10px; }

.user-avatar {
  flex-shrink: 0;
  border: 2px solid rgba(99, 102, 241, 0.3);
}

.user-name { font-size: 13px; color: #E2E8F0; display: block; font-weight: 500; }
.user-username { font-size: 11px; color: #475569; display: block; }
.cell-primary { font-size: 13px; color: #CBD5E1; }
.cell-sub { font-size: 11px; color: #475569; }

.perm-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.no-perm { font-size: 12px; color: #475569; }

.action-btns { display: flex; align-items: center; gap: 6px; justify-content: flex-end; }

.op-btn {
  border-radius: 6px !important;
  
  &.edit {
    background: rgba(99, 102, 241, 0.1) !important;
    border: 1px solid rgba(99, 102, 241, 0.2) !important;
    color: #818CF8 !important;
  }
  &.perm {
    background: rgba(16, 185, 129, 0.08) !important;
    border: 1px solid rgba(16, 185, 129, 0.2) !important;
    color: #10B981 !important;
  }
  &.more {
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.08) !important;
    color: #64748B !important;
  }
}

// 权限弹窗
.perm-dialog-content { padding: 4px 0; }

.perm-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.perm-username { font-size: 14px; color: #F1F5F9; font-weight: 500; }
.perm-dept { font-size: 12px; color: #64748B; margin-top: 2px; }

.perm-divider {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  margin-bottom: 16px;
}

.perm-hint { font-size: 12px; color: #64748B; margin-bottom: 12px; }

.perm-checkboxes { display: flex; flex-direction: column; gap: 10px; }

.perm-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.03);
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover { background: rgba(255, 255, 255, 0.06); }
  
  &.checked {
    border-color: rgba(99, 102, 241, 0.3);
    background: rgba(99, 102, 241, 0.06);
  }
}

.perm-item-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  
  &.model { background: rgba(99, 102, 241, 0.15); color: #818CF8; }
  &.knowledge { background: rgba(16, 185, 129, 0.12); color: #10B981; }
  &.user { background: rgba(245, 158, 11, 0.12); color: #F59E0B; }
}

.perm-item-info { flex: 1; }
.perm-item-label { font-size: 13px; color: #E2E8F0; font-weight: 500; display: block; }
.perm-item-sub { font-size: 11px; color: #475569; display: block; margin-top: 2px; }
</style>
