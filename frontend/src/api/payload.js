import request from '@/utils/request'

export const getGadgets = () => {
  return request.get('/payload/gadgets')
}

export const generatePayload = (data) => {
  return request.post('/payload/generate', data)
}

export const testPayload = (data) => {
  return request.post('/payload/test', data)
}

export const getHistory = () => {
  return request.get('/payload/history')
}

export const deleteHistory = (id) => {
  return request.delete(`/payload/history/${id}`)
}
