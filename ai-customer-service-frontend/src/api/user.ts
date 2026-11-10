import request from '@/utils/request'

export const getUserList = () => request.get('/admin/users')
export const createUser = (data: any) => request.post('/admin/users', data)
export const updateUser = (id: number, data: any) => request.put(`/admin/users/${id}`, data)
export const deleteUser = (id: number) => request.delete(`/admin/users/${id}`)
export const toggleUser = (id: number) => request.patch(`/admin/users/${id}/toggle`)
export const resetPassword = (id: number, newPassword: string) =>
  request.patch(`/admin/users/${id}/reset-password`, { newPassword })
export const assignPermissions = (id: number, perms: {
  permModel: boolean
  permKnowledge: boolean
  permUser: boolean
}) => request.patch(`/admin/users/${id}/permissions`, perms)
