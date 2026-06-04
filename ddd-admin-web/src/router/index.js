import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'

// 懒加载页面
const LoginPage = () => import('@/views/login/LoginPage.vue')
const Layout = () => import('@/layout/Layout.vue')
const DashboardPage = () => import('@/views/dashboard/DashboardPage.vue')
const UserListPage = () => import('@/views/user/UserListPage.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: DashboardPage,
        meta: { title: '仪表盘', icon: 'Monitor' }
      },
      {
        path: 'user',
        name: 'User',
        component: UserListPage,
        meta: { title: '用户管理', icon: 'User' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', noAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', noAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ======================== 路由守卫 ========================
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - DDD Admin` : 'DDD Admin'

  const token = getToken()

  // 1. 访问登录页
  if (to.path === '/login') {
    if (token) {
      // 已登录 → 跳转首页
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  // 2. 免认证页面（403/404）
  if (to.meta.noAuth) {
    next()
    return
  }

  // 3. 需要认证的页面
  if (!token) {
    // 无 Token → 跳转登录页（带上重定向地址）
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
