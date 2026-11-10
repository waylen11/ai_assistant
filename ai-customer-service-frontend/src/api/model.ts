import request from '@/utils/request'

export const getEnabledModels = () => request.get('/models/enabled')
export const getAllModels = () => request.get('/admin/models')
export const createModel = (data: any) => request.post('/admin/models', data)
export const updateModel = (id: number, data: any) => request.put(`/admin/models/${id}`, data)
export const deleteModel = (id: number) => request.delete(`/admin/models/${id}`)
export const toggleModel = (id: number) => request.patch(`/admin/models/${id}/toggle`)
