<template>
  <el-select
    v-model="selectedModelName"
    placeholder="选择模型"
    class="model-selector"
    @change="handleChange"
  >
    <template #prefix>
      <!-- 已选中模型的图标 -->
      <div v-if="modelStore.currentModel" class="prefix-model-icon">
        <img
          v-if="modelStore.currentModel.iconUrl && !brokenIcons.has(modelStore.currentModel.id)"
          :src="modelStore.currentModel.iconUrl"
          :alt="modelStore.currentModel.modelName"
          class="prefix-img"
          @error="brokenIcons.add(modelStore.currentModel!.id)"
        />
        <span
          v-else
          class="prefix-letter"
          :style="{ background: getProviderGradient(modelStore.currentModel.provider) }"
        >{{ getProviderLetter(modelStore.currentModel.provider) }}</span>
      </div>
      <el-icon v-else class="prefix-icon"><Cpu /></el-icon>
    </template>

    <el-option
      v-for="model in modelStore.models"
      :key="model.id"
      :label="model.modelName"
      :value="model.modelName"
    >
      <div class="model-option">
        <!-- 模型图标：有 iconUrl 且未损坏则显示图片，否则显示品牌色首字母 -->
        <div class="model-icon-wrap">
          <img
            v-if="model.iconUrl && !brokenIcons.has(model.id)"
            :src="model.iconUrl"
            :alt="model.modelName"
            class="model-icon"
            @error="brokenIcons.add(model.id)"
          />
          <span
            v-else
            class="model-icon-letter"
            :style="{ background: getProviderGradient(model.provider) }"
          >{{ getProviderLetter(model.provider) }}</span>
        </div>

        <div class="model-info">
          <span class="model-name">{{ model.modelName }}</span>
          <span class="model-provider">{{ model.provider }}</span>
        </div>
        <el-tag v-if="model.multimodal" size="small" type="warning" class="multimodal-tag">多模态</el-tag>
      </div>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useModelStore } from '@/stores/model'
import { Cpu } from '@element-plus/icons-vue'

const modelStore = useModelStore()
const selectedModelName = ref('')

// 记录加载失败的图标 id，避免反复重试
const brokenIcons = reactive(new Set<number>())

onMounted(async () => {
  await modelStore.fetchModels()
  if (modelStore.currentModel) {
    selectedModelName.value = modelStore.currentModel.modelName
  }
})

function handleChange(val: string) {
  const model = modelStore.models.find(m => m.modelName === val)
  if (model) modelStore.selectModel(model)
}

/** 根据 provider 名称返回品牌渐变色 */
function getProviderGradient(provider: string): string {
  const p = (provider || '').toLowerCase()
  if (p.includes('qwen') || p.includes('阿里') || p.includes('alibaba') || p.includes('bailian') || p.includes('百炼')) {
    return 'linear-gradient(135deg, #FF6A00, #EE0979)'
  }
  if (p.includes('deepseek')) {
    return 'linear-gradient(135deg, #3B82F6, #1D4ED8)'
  }
  if (p.includes('openai') || p.includes('gpt')) {
    return 'linear-gradient(135deg, #10A37F, #1A7A60)'
  }
  if (p.includes('claude') || p.includes('anthropic')) {
    return 'linear-gradient(135deg, #CC785C, #A0522D)'
  }
  if (p.includes('gemini') || p.includes('google')) {
    return 'linear-gradient(135deg, #4285F4, #0F9D58)'
  }
  // 默认紫色
  return 'linear-gradient(135deg, #6366F1, #4F46E5)'
}

/** 取 provider 首字母作为 Avatar 文字（英文取首字母大写，中文按品牌映射） */
function getProviderLetter(provider: string): string {
  const p = (provider || '').toLowerCase()
  if (p.includes('qwen') || p.includes('阿里') || p.includes('alibaba') || p.includes('bailian') || p.includes('百炼')) return 'Q'
  if (p.includes('deepseek')) return 'D'
  if (p.includes('openai') || p.includes('gpt')) return 'G'
  if (p.includes('claude') || p.includes('anthropic')) return 'C'
  if (p.includes('gemini') || p.includes('google')) return 'G'
  // fallback：取首个大写字母
  const first = (provider || '').charAt(0).toUpperCase()
  return first || 'A'
}
</script>

<style lang="scss" scoped>
.model-selector {
  width: 180px;

  :deep(.el-select__wrapper) {
    background: rgba(255, 255, 255, 0.06) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
    border-radius: 20px !important;
    padding: 0 12px !important;
  }

  :deep(.el-select__selected-item) {
    color: #F1F5F9 !important;
    font-size: 13px;
  }
}

/* 穿透 el-option，放大行高和内边距让图标完整显示 */
:deep(.el-select-dropdown__item) {
  height: auto !important;
  line-height: 1 !important;
  padding: 8px 12px !important;
}

/* ── 已选中模型前缀图标 ── */
.prefix-model-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
}

.prefix-img {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  object-fit: cover;
}

.prefix-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.prefix-icon {
  color: #6366F1;
  font-size: 14px;
}

/* ── 下拉列表选项 ── */
.model-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
  min-height: 36px;
}

.model-icon-wrap {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  overflow: hidden;
}

.model-icon {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.model-icon-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.model-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.model-name {
  font-size: 13px;
  color: #F1F5F9;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-provider {
  font-size: 11px;
  color: #64748B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.multimodal-tag {
  font-size: 10px;
  height: 18px;
  padding: 0 4px;
  flex-shrink: 0;
}
</style>
