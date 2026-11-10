import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getEnabledModels } from '@/api/model'

export interface ModelItem {
  id: number
  modelType: string
  modelName: string
  provider: string
  description: string
  multimodal: boolean
  iconUrl: string
  enabled: boolean
}

export const useModelStore = defineStore('model', () => {
  const models = ref<ModelItem[]>([])
  const currentModel = ref<ModelItem | null>(null)
  const loading = ref(false)

  async function fetchModels() {
    loading.value = true
    try {
      const res = await getEnabledModels()
      models.value = res.data || []
      if (models.value.length && !currentModel.value) {
        currentModel.value = models.value[0]
      }
    } finally {
      loading.value = false
    }
  }

  function selectModel(model: ModelItem) {
    currentModel.value = model
  }

  return { models, currentModel, loading, fetchModels, selectModel }
})
