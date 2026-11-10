<template>
  <div class="knowledge-manage-page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">知识库管理</h2>
        <p class="page-sub">上传文档，自动分块向量化，赋能 RAG 智能检索</p>
      </div>
    </div>

    <!-- 上传区域 -->
    <div
      class="upload-zone glass-card"
      :class="{ 'drag-over': isDragging }"
      @dragover.prevent="isDragging = true"
      @dragleave="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <div class="upload-inner">
        <div class="upload-icon">
          <el-icon size="40"><UploadFilled /></el-icon>
        </div>
        <div class="upload-text">
          <p class="upload-title">拖拽文件到此处上传</p>
          <p class="upload-sub">支持 PDF、Word（.docx）、TXT、Markdown 文件，单文件最大 50MB</p>
        </div>
        <el-button class="select-btn" :loading="uploading" @click="triggerFileInput">
          <el-icon><Plus /></el-icon> 选择文件
        </el-button>
        <input ref="fileInputRef" type="file" accept=".pdf,.docx,.txt,.md" style="display:none" @change="handleFileSelect" />
      </div>

      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-progress">
        <el-progress
          :percentage="uploadProgress"
          :format="() => `${uploadProgress}% 上传中...`"
          :stroke-width="6"
          color="#6366F1"
        />
      </div>
    </div>

    <!-- Chunk 分割预览 -->
    <div v-if="chunkPreviews.length > 0" class="chunk-preview-section">
      <div class="section-header">
        <h3 class="section-title">
          <el-icon><Files /></el-icon>
          文档分块预览
          <el-tag type="info" size="small">共 {{ chunkPreviews.length }} 个分块</el-tag>
        </h3>
        <p class="section-sub">以下是文档按语义分割后的预览，将全部向量化写入 Milvus</p>
      </div>
      <div class="chunk-grid">
        <div
          v-for="chunk in chunkPreviews"
          :key="chunk.index"
          class="chunk-card glass-card"
        >
          <div class="chunk-header">
            <span class="chunk-index">#{{ chunk.index }}</span>
            <span class="chunk-chars">{{ chunk.charCount }} 字</span>
          </div>
          <p class="chunk-content">{{ chunk.content }}</p>
        </div>
      </div>
    </div>

    <!-- 知识库列表 -->
    <div class="kb-list-section">
      <div class="section-header">
        <h3 class="section-title">
          <el-icon><List /></el-icon>
          已上传文档
        </h3>
        <el-button size="small" class="refresh-btn" @click="fetchList">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>

      <el-table :data="kbList" class="kb-table" v-loading="listLoading">
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-info">
              <div class="file-icon" :class="row.fileType">
                <el-icon><Document /></el-icon>
              </div>
              <div>
                <div class="file-name">{{ row.fileName }}</div>
                <div class="file-size">{{ formatSize(row.fileSize) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分块数" prop="chunkCount" width="90" align="center" />
        <el-table-column label="向量化状态" width="160" align="center">
          <template #default="{ row }">
            <div class="vector-status" :class="row.vectorStatus">
              <el-icon v-if="row.vectorStatus === 'processing'" class="spin"><Loading /></el-icon>
              <el-icon v-else-if="row.vectorStatus === 'completed'"><CircleCheck /></el-icon>
              <el-icon v-else-if="row.vectorStatus === 'failed'"><CircleClose /></el-icon>
              <el-icon v-else><Clock /></el-icon>
              {{ statusLabel(row.vectorStatus) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button size="small" class="del-btn" @click="handleDelete(row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { uploadKnowledge, getKnowledgeList, deleteKnowledge } from '@/api/knowledge'

const fileInputRef = ref<HTMLInputElement>()
const isDragging = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const chunkPreviews = ref<any[]>([])
const kbList = ref<any[]>([])
const listLoading = ref(false)

onMounted(fetchList)

async function fetchList() {
  listLoading.value = true
  try {
    const res = await getKnowledgeList() as any
    kbList.value = res.data || []
  } finally {
    listLoading.value = false
  }
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) uploadFile(file)
}

function handleDrop(e: DragEvent) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) uploadFile(file)
}

async function uploadFile(file: File) {
  const MAX_SIZE = 50 * 1024 * 1024
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件大小不能超过 50MB')
    return
  }
  uploading.value = true
  uploadProgress.value = 0
  chunkPreviews.value = []

  // 模拟进度
  const timer = setInterval(() => {
    if (uploadProgress.value < 85) uploadProgress.value += 10
  }, 300)

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadKnowledge(formData) as any
    uploadProgress.value = 100
    chunkPreviews.value = res.data?.chunks || []
    ElMessage.success(`上传成功！共分割 ${chunkPreviews.value.length} 个分块，正在后台向量化...`)
    fetchList()
  } catch {
    ElMessage.error('上传失败，请重试')
  } finally {
    clearInterval(timer)
    setTimeout(() => { uploading.value = false }, 800)
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该知识库文档？', '提示', { type: 'warning' })
  await deleteKnowledge(id)
  ElMessage.success('已删除')
  fetchList()
}

function statusLabel(s: string) {
  const map: Record<string, string> = { pending: '待处理', processing: '处理中', completed: '已完成', failed: '处理失败' }
  return map[s] || s
}

function formatSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(d: string) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style lang="scss" scoped>
.knowledge-manage-page {
  height: 100%;
  overflow-y: auto;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header { margin-bottom: 4px; }
.page-title { font-size: 20px; font-weight: 600; color: #F1F5F9; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: #64748B; }

// 上传区
.upload-zone {
  padding: 40px;
  border: 2px dashed rgba(255, 255, 255, 0.12);
  text-align: center;
  transition: all 0.3s;
  
  &.drag-over {
    border-color: #6366F1;
    background: rgba(99, 102, 241, 0.08) !important;
  }
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  width: 72px;
  height: 72px;
  background: rgba(99, 102, 241, 0.1);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366F1;
}

.upload-title { font-size: 15px; color: #CBD5E1; font-weight: 500; }
.upload-sub { font-size: 12px; color: #475569; margin-top: 4px; }

.select-btn {
  background: rgba(99, 102, 241, 0.15) !important;
  border: 1px solid rgba(99, 102, 241, 0.3) !important;
  color: #818CF8 !important;
  border-radius: 20px !important;
  padding: 8px 24px !important;
  margin-top: 4px;
}

.upload-progress { margin-top: 16px; width: 100%; max-width: 400px; }

// 分块预览
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #F1F5F9;
  
  .el-icon { color: #6366F1; }
}

.section-sub { font-size: 12px; color: #475569; margin-top: 4px; }

.chunk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.chunk-card {
  padding: 14px;
}

.chunk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.chunk-index {
  font-size: 11px;
  font-weight: 600;
  color: #6366F1;
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
}

.chunk-chars { font-size: 11px; color: #475569; }

.chunk-content {
  font-size: 12px;
  color: #64748B;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// 知识库列表
.kb-list-section { flex: 1; }

.refresh-btn {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  color: #64748B !important;
  border-radius: 8px !important;
}

.file-info { display: flex; align-items: center; gap: 10px; }

.file-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366F1;
  font-size: 16px;
}

.file-name { font-size: 13px; color: #E2E8F0; }
.file-size { font-size: 11px; color: #475569; }

.vector-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 10px;
  
  &.pending { color: #94A3B8; background: rgba(148, 163, 184, 0.1); }
  &.processing { color: #F59E0B; background: rgba(245, 158, 11, 0.1); }
  &.completed { color: #10B981; background: rgba(16, 185, 129, 0.1); }
  &.failed { color: #EF4444; background: rgba(239, 68, 68, 0.1); }
}

.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }

.del-btn {
  background: rgba(239, 68, 68, 0.08) !important;
  border: 1px solid rgba(239, 68, 68, 0.15) !important;
  color: #EF4444 !important;
  border-radius: 6px !important;
}
</style>
