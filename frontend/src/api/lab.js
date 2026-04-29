import request from '@/utils/request'

export const getTargets = () => {
  return request.get('/target/list')
}

export const saveTarget = (data) => {
  return request.post('/target/save', data)
}

export const deleteTarget = (id) => {
  return request.delete(`/target/delete/${id}`)
}
