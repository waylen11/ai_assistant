import request from '@/utils/request'

export const getKnowledgeList = () => request.get('/admin/knowledge')
export const uploadKnowledge = (formData: FormData) =>
  request.post('/admin/knowledge/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
export const deleteKnowledge = (id: number) => request.delete(`/admin/knowledge/${id}`)
