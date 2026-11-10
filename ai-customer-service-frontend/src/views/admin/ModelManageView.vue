<template>
  <div class="model-manage-page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">模型管理</h2>
        <p class="page-sub">管理系统中所有 AI 大模型配置</p>
      </div>
      <el-button class="add-btn" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新增模型
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input v-model="searchText" placeholder="搜索模型名称 / 厂商" class="search-input" clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-area">
      <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
    </div>

    <!-- 4列模型网格 -->
    <div v-else class="model-grid">
      <div
        v-for="model in filteredModels"
        :key="model.id"
        class="model-card glass-card-hover"
      >
        <!-- 卡片顶部 -->
        <div class="card-top">
          <div class="model-avatar" :style="(!model.iconUrl || brokenIcons.has(model.id)) ? { background: getProviderGradient(model.provider) } : {}">
            <img
              v-if="model.iconUrl && !brokenIcons.has(model.id)"
              :src="model.iconUrl"
              :alt="model.modelName"
              @error="brokenIcons.add(model.id)"
            />
            <span v-else class="avatar-letter">{{ getProviderLetter(model.provider) }}</span>
          </div>
          <div class="card-badges">
            <el-tag v-if="model.multimodal" size="small" type="warning">多模态</el-tag>
            <el-switch
              v-model="model.enabled"
              size="small"
              @change="toggleModel(model)"
            />
          </div>
        </div>

        <!-- 模型信息 -->
        <div class="model-body">
          <h3 class="model-name">{{ model.modelName }}</h3>
          <div class="model-provider">
            <el-icon><OfficeBuilding /></el-icon>
            {{ model.provider }}
          </div>
          <p class="model-desc">{{ model.description || '暂无描述' }}</p>
        </div>

        <!-- 卡片底部操作 -->
        <div class="card-footer">
          <el-button size="small" class="edit-btn" @click="openDialog(model)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button size="small" class="del-btn" @click="handleDelete(model.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredModels.length === 0" class="empty-state">
        <el-icon size="48"><Cpu /></el-icon>
        <p>暂无模型，点击"新增模型"添加</p>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editModel?.id ? '编辑模型' : '新增模型'"
      width="600px"
      class="model-dialog"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="110px" class="model-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="大模型类型" prop="modelType">
              <el-input v-model="formData.modelType" placeholder="如 qwen" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="formData.modelName" placeholder="如 qwen-max" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提供厂商" prop="provider">
              <el-input v-model="formData.provider" placeholder="如 阿里云百炼" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否多模态">
              <el-switch v-model="formData.multimodal" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="API Key" prop="apiKey">
              <el-input v-model="formData.apiKey" type="password" show-password placeholder="请输入 API Key" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Base URL">
              <el-input v-model="formData.baseUrl" placeholder="自定义 API 地址（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图标 URL">
              <el-input v-model="formData.iconUrl" placeholder="模型图标地址（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="优势描述">
              <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="描述该模型的特点和优势" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序权重">
              <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否启用">
              <el-switch v-model="formData.enabled" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { getAllModels, createModel, updateModel, deleteModel, toggleModel as toggleModelApi } from '@/api/model'
import type { FormInstance, FormRules } from 'element-plus'

const models = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const searchText = ref('')
const dialogVisible = ref(false)
const editModel = ref<any>(null)
const formRef = ref<FormInstance>()
const brokenIcons = reactive(new Set<number>())

const formData = reactive({
  modelType: '', modelName: '', provider: '', description: '',
  apiKey: '', baseUrl: '', multimodal: false, iconUrl: '',
  enabled: true, sortOrder: 0,
})

const formRules: FormRules = {
  modelType: [{ required: true, message: '请输入模型类型' }],
  modelName: [{ required: true, message: '请输入模型名称' }],
  provider: [{ required: true, message: '请输入提供厂商' }],
  apiKey: [{ required: true, message: '请输入 API Key' }],
}

const filteredModels = computed(() =>
  models.value.filter(m =>
    !searchText.value ||
    m.modelName.toLowerCase().includes(searchText.value.toLowerCase()) ||
    m.provider.toLowerCase().includes(searchText.value.toLowerCase())
  )
)

onMounted(fetchModels)

async function fetchModels() {
  loading.value = true
  try {
    const res = await getAllModels() as any
    models.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(model?: any) {
  editModel.value = model || null
  if (model) {
    Object.assign(formData, model)
  } else {
    Object.assign(formData, { modelType: '', modelName: '', provider: '', description: '', apiKey: '', baseUrl: '', multimodal: false, iconUrl: '', enabled: true, sortOrder: 0 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    if (editModel.value?.id) {
      await updateModel(editModel.value.id, formData)
      ElMessage.success('模型更新成功')
    } else {
      await createModel(formData)
      ElMessage.success('模型创建成功')
    }
    dialogVisible.value = false
    fetchModels()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该模型？', '提示', { type: 'warning' })
  await deleteModel(id)
  ElMessage.success('已删除')
  fetchModels()
}

async function toggleModel(model: any) {
  await toggleModelApi(model.id)
  ElMessage.success(model.enabled ? '已启用' : '已禁用')
}

// 根据 provider 关键词映射渐变色
const PROVIDER_GRADIENTS: Record<string, string> = {
  '阿里': 'linear-gradient(135deg, #FF6A00, #EE0979)',
  'ali': 'linear-gradient(135deg, #FF6A00, #EE0979)',
  'qwen': 'linear-gradient(135deg, #FF6A00, #EE0979)',
  'openai': 'linear-gradient(135deg, #10A37F, #0D8C6D)',
  'gpt': 'linear-gradient(135deg, #10A37F, #0D8C6D)',
  'claude': 'linear-gradient(135deg, #D97706, #B45309)',
  'anthropic': 'linear-gradient(135deg, #D97706, #B45309)',
  'gemini': 'linear-gradient(135deg, #4285F4, #0F9D58)',
  'google': 'linear-gradient(135deg, #4285F4, #0F9D58)',
  'deepseek': 'linear-gradient(135deg, #3B82F6, #1D4ED8)',
  '深度求索': 'linear-gradient(135deg, #3B82F6, #1D4ED8)',
  'moonshot': 'linear-gradient(135deg, #7C3AED, #4C1D95)',
  '月之暗面': 'linear-gradient(135deg, #7C3AED, #4C1D95)',
  'zhipu': 'linear-gradient(135deg, #06B6D4, #0284C7)',
  '智谱': 'linear-gradient(135deg, #06B6D4, #0284C7)',
  'baidu': 'linear-gradient(135deg, #2563EB, #1E40AF)',
  '百度': 'linear-gradient(135deg, #2563EB, #1E40AF)',
  'hunyuan': 'linear-gradient(135deg, #059669, #047857)',
  '腾讯': 'linear-gradient(135deg, #059669, #047857)',
}

function getProviderGradient(provider: string): string {
  if (!provider) return 'linear-gradient(135deg, #6366F1, #8B5CF6)'
  const lower = provider.toLowerCase()
  for (const key of Object.keys(PROVIDER_GRADIENTS)) {
    if (lower.includes(key.toLowerCase())) return PROVIDER_GRADIENTS[key]
  }
  // 根据字符串哈希生成稳定的渐变色
  const colors = [
    'linear-gradient(135deg, #6366F1, #8B5CF6)',
    'linear-gradient(135deg, #EC4899, #DB2777)',
    'linear-gradient(135deg, #14B8A6, #0D9488)',
    'linear-gradient(135deg, #F59E0B, #D97706)',
    'linear-gradient(135deg, #EF4444, #DC2626)',
    'linear-gradient(135deg, #8B5CF6, #7C3AED)',
  ]
  let hash = 0
  for (let i = 0; i < provider.length; i++) hash = (hash * 31 + provider.charCodeAt(i)) & 0xFFFFFF
  return colors[Math.abs(hash) % colors.length]
}

function getProviderLetter(provider: string): string {
  if (!provider) return 'AI'
  // 中文取前两个字，英文取前两个大写字母
  const trimmed = provider.trim()
  const isChinese = /[\u4e00-\u9fa5]/.test(trimmed)
  if (isChinese) return trimmed.slice(0, 2)
  return trimmed.slice(0, 2).toUpperCase()
}
</script>

<style lang="scss" scoped>
.model-manage-page {
  height: 100%;
  overflow-y: auto;
  padding: 4px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #F1F5F9;
  margin-bottom: 4px;
}

.page-sub {
  font-size: 13px;
  color: #64748B;
}

.add-btn {
  background: linear-gradient(135deg, #6366F1, #8B5CF6) !important;
  border: none !important;
  color: white !important;
  border-radius: 10px !important;
}

.toolbar { margin-bottom: 20px; }

.search-input {
  width: 300px;
  :deep(.el-input__wrapper) { border-radius: 20px !important; }
}

.loading-area {
  display: flex;
  justify-content: center;
  padding: 80px;
  
  .loading-icon {
    color: #6366F1;
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

// 4列网格
.model-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  
  @media (max-width: 1280px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 900px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 600px) { grid-template-columns: 1fr; }
}

.model-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.model-avatar {
  width: 52px;
  height: 52px;
  background: rgba(99, 102, 241, 0.1);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366F1;
  overflow: hidden;
  flex-shrink: 0;
  
  img { width: 100%; height: 100%; object-fit: cover; }
}

.avatar-letter {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
  line-height: 1;
  user-select: none;
}

.card-badges {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.model-body { flex: 1; }

.model-name {
  font-size: 15px;
  font-weight: 600;
  color: #F1F5F9;
  margin-bottom: 4px;
}

.model-provider {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #64748B;
  margin-bottom: 8px;
}

.model-desc {
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  gap: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding-top: 10px;
}

.edit-btn {
  flex: 1;
  background: rgba(99, 102, 241, 0.1) !important;
  border: 1px solid rgba(99, 102, 241, 0.2) !important;
  color: #818CF8 !important;
  border-radius: 8px !important;
}

.del-btn {
  background: rgba(239, 68, 68, 0.08) !important;
  border: 1px solid rgba(239, 68, 68, 0.2) !important;
  color: #EF4444 !important;
  border-radius: 8px !important;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
  color: #475569;
  font-size: 14px;
}

.model-form :deep(.el-form-item__label) { color: #94A3B8; }
</style>
