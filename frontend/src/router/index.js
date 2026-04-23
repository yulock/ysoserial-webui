import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserInfo } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/views/Layout.vue'),
      redirect: '/payload',
      children: [
        {
          path: '/payload',
          name: 'Payload',
          component: () => import('@/views/Payload.vue'),
          meta: { title: 'Payload生成', icon: 'Magic' }
        },
        {
          path: '/history',
          name: 'History',
          component: () => import('@/views/History.vue'),
          meta: { title: '历史记录', icon: 'Clock' }
        },
        {
          path: '/lab',
          name: 'Lab',
          component: () => import('@/views/Lab.vue'),
          meta: { title: '实验室', icon: 'Collection' }
        },
        {
          path: '/profile',
          name: 'Profile',
          component: () => import('@/views/Profile.vue'),
          meta: { title: '个人中心', icon: 'User' }
        }
      ]
    }
  ]
})

// 是否需要验证token有效性
let hasValidatedToken = false

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 如果是公开页面，直接放行
  if (to.meta.public) {
    // 如果已登录且访问登录页，跳转到首页
    if (to.path === '/login' && userStore.token && userStore.userInfo.username) {
      next('/')
      return
    }
    next()
    return
  }
  
  // 没有token，跳转到登录页
  if (!userStore.token) {
    next('/login')
    return
  }
  
  // 有token但未验证过有效性，尝试获取用户信息
  if (!hasValidatedToken && !userStore.userInfo.username) {
    try {
      await getUserInfo()
      hasValidatedToken = true
      next()
    } catch (error) {
      // token无效，清除登录状态并跳转
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      next('/login')
    }
    return
  }
  
  next()
})

export default router
