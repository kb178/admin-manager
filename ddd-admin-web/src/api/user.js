import request from './request'

/**
 * 获取当前登录用户信息
 */
export function getUserInfo() {
  return request.get('/user/me')
}

/**
 * 分页查询用户列表
 * @param {Object} params - { username, realName, phone, status, page, pageSize }
 */
export function getUserList(params) {
  return request.get('/user/list', { params })
}

/**
 * 根据 ID 获取用户详情
 * @param {number} id
 */
export function getUserById(id) {
  return request.get(`/user/${id}`)
}

/**
 * 创建用户
 * @param {Object} data
 */
export function createUser(data) {
  return request.post('/user', data)
}

/**
 * 更新用户
 * @param {number} id
 * @param {Object} data
 */
export function updateUser(id, data) {
  return request.put(`/user/${id}`, data)
}

/**
 * 删除用户（逻辑删除）
 * @param {number} id
 */
export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}

/**
 * 修改密码
 * @param {string} oldPassword
 * @param {string} newPassword
 */
export function changePassword(oldPassword, newPassword) {
  return request.put('/user/change-password', { oldPassword, newPassword })
}

/**
 * 重置用户密码（管理员专用）
 * @param {number} id
 * @param {string} newPassword
 */
export function resetPassword(id, newPassword) {
  return request.put(`/user/${id}/reset-password`, { newPassword })
}
