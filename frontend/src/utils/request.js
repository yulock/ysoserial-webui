import axios from 'axios'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 是否在刷新token的标记
let isRefreshing = false
// 等待的队列
let requestsQueue = []

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = userStore.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      // 处理未登录或token无效
      if (res.code === 401 || res.code === -1) {
        const userStore = useUserStore()
        // 如果不是登录页面，才提示并跳转
        if (!window.location.pathname.includes('/login')) {
          ElMessage.error('登录已过期，请重新登录')
          userStore.logout()
          window.location.href = '/login'
        }
        return Promise.reject(new Error(res.msg || '未登录'))
      }
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  (error) => {
    const { response } = error
    
    if (response) {
      // 处理HTTP状态码
      switch (response.status) {
        case 401:
          // token失效
          const userStore = useUserStore()
          if (!window.location.pathname.includes('/login')) {
            ElMessage.error('登录已过期，请重新登录')
            userStore.logout()
            window.location.href = '/login'
          }
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(response.data?.msg || '请求失败')
      }
    } else {
      // 网络错误或服务未启动
      if (error.message.includes('Network Error')) {
        ElMessage.error('网络错误或服务器未启动')
      } else if (error.message.includes('timeout')) {
        ElMessage.error('请求超时，请稍后重试')
      } else {
        ElMessage.error(error.message || '网络错误')
      }
    }
    
    return Promise.reject(error)
  }
)

export default request
