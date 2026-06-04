import axios from 'axios'
import { getToken, setToken, setRefreshToken, removeToken, removeRefreshToken } from '@/utils/token'
import { ElMessage } from 'element-plus'

// 创建 Axios 实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ======================== 请求拦截器 ========================
request.interceptors.request.use(
  (config) => {
    // 自动附带 Token
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ======================== 响应拦截器 ========================
// 是否正在刷新 Token（防止并发请求同时刷新）
let isRefreshing = false
// 等待刷新的请求队列
let refreshQueue = []

/**
 * 处理刷新后的请求重放
 */
function onRefreshed(newToken) {
  refreshQueue.forEach(({ resolve }) => resolve(newToken))
  refreshQueue = []
}

/**
 * 处理刷新失败
 */
function onRefreshFailed(error) {
  refreshQueue.forEach(({ reject }) => reject(error))
  refreshQueue = []
}

request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 后端直接返回 Result 对象 { code, message, data }
    if (res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  async (error) => {
    // 网络错误或超时
    if (!error.response) {
      ElMessage.error('网络异常，请检查网络连接')
      return Promise.reject(error)
    }

    const { status, config } = error.response

    // 401 未授权 — 尝试刷新 Token
    if (status === 401) {
      // 排除登录接口本身和刷新接口
      if (config.url?.includes('/auth/login') || config.url?.includes('/auth/refresh')) {
        removeToken()
        removeRefreshToken()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      // 如果正在刷新，将当前请求加入队列
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject })
        }).then(() => request(config))
      }

      isRefreshing = true

      try {
        // 从 localStorage 取 Refresh Token
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) {
          throw new Error('无 Refresh Token')
        }

        // 调用刷新接口
        const res = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL || '/api'}/auth/refresh`,
          { refreshToken }
        )

        if (res.data.code === 200) {
          const newToken = res.data.data.accessToken
          const newRefreshToken = res.data.data.refreshToken

          setToken(newToken)
          setRefreshToken(newRefreshToken)

          // 重放排队的请求
          onRefreshed(newToken)

          // 重试当前请求
          config.headers.Authorization = `Bearer ${newToken}`
          return request(config)
        } else {
          throw new Error('刷新 Token 失败')
        }
      } catch (refreshError) {
        onRefreshFailed(refreshError)
        // 清除登录状态
        removeToken()
        removeRefreshToken()
        ElMessage.error('登录已过期，请重新登录')
        // 跳转登录页
        setTimeout(() => {
          window.location.href = '/login'
        }, 1500)
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 403 无权限
    if (status === 403) {
      ElMessage.error('没有操作权限')
    }

    // 400 参数错误
    if (status === 400) {
      const msg = error.response.data?.message || '请求参数错误'
      ElMessage.error(msg)
    }

    // 500 服务器错误
    if (status >= 500) {
      ElMessage.error('服务器异常，请稍后再试')
    }

    return Promise.reject(error)
  }
)

export default request
