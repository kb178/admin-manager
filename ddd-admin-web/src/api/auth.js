import request from './request'

/**
 * 获取图形验证码
 */
export function getCaptcha() {
  return request.get('/auth/captcha')
}

/**
 * 登录
 * @param {Object} data - { username, password, captcha, captchaKey }
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 刷新 Token
 * @param {string} refreshToken
 */
export function refreshTokenApi(refreshToken) {
  return request.post('/auth/refresh', { refreshToken })
}

/**
 * 退出登录
 */
export function logout() {
  return request.post('/auth/logout')
}
