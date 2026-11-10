import request from '@/utils/request'

export const getSessions = () => request.get('/assistant/sessions')
export const createSession = (data?: { modelName?: string }) =>
  request.post('/assistant/sessions', data || {})
export const deleteSession = (sessionId: string) =>
  request.delete(`/assistant/sessions/${sessionId}`)
export const getHistory = () => request.get('/chat/history')
