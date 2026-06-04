import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, refreshTokenApi, logout as logoutApi, getCaptcha } from '@/api/auth'
import { getUserInfo } from '@/api/user'
import { getToken, setToken, removeToken, getRefreshToken, setRefreshToken, removeRefreshToken } from '@/utils/token'
import { ElMessage } from 'element-plus'

/**
 * 认证 Store — 管理登录状态、Token、用户信息
 */
export const useAuthStore = defineStore('auth', () => {
  // ======================== State ========================

  const token = ref(getToken() || '')
  const refreshToken = ref(getRefreshToken() || '')
  const userInfo = ref(null)
  const captchaKey = ref('')

  // ======================== Getters ========================

  const isAuthenticated = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || username.value)
  const roles = computed(() => userInfo.value?.roles || [])
  const permissions = computed(() => userInfo.value?.permissions || [])
  const avatar = computed(() => userInfo.value?.avatar || '')

  /**
   * 是否拥有某个角色
   */
  function hasRole(role) {
    return roles.value.includes(role)
  }

  /**
   * 是否拥有某个权限
   */
  function hasPermission(perm) {
    return permissions.value.includes(perm)
  }

  // ======================== Actions ========================

  /**
   * 获取验证码
   */
  async function fetchCaptcha() {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    return res.data
  }

  /**
   * 登录
   */
  async function login(form) {
    const res = await loginApi({
      username: form.username,
      password: form.password,
      captcha: form.captcha,
      captchaKey: captchaKey.value
    })

    const data = res.data
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    userInfo.value = data.userInfo

    // 持久化存储
    setToken(data.accessToken)
    setRefreshToken(data.refreshToken)

    ElMessage.success('登录成功')
    return data
  }

  /**
   * 获取当前用户信息
   */
  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data.user
    return res.data
  }

  /**
   * 刷新 Access Token
   */
  async function refreshAccessToken() {
    if (!refreshToken.value) {
      throw new Error('无 Refresh Token')
    }
    const res = await refreshTokenApi(refreshToken.value)
    const data = res.data
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    setToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    return data
  }

  /**
   * 退出登录
   */
  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 即使接口失败也清除本地状态
    } finally {
      resetState()
      ElMessage.success('已退出登录')
    }
  }

  /**
   * 重置所有状态
   */
  function resetState() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    captchaKey.value = ''
    removeToken()
    removeRefreshToken()
  }

  return {
    // state
    token, refreshToken, userInfo, captchaKey,
    // getters
    isAuthenticated, username, realName, roles, permissions, avatar,
    // methods
    hasRole, hasPermission, fetchCaptcha, login, fetchUserInfo,
    refreshAccessToken, logout, resetState
  }
})
