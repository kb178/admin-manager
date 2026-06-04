import request from './request'

/**
 * 获取系统硬件信息（CPU/GPU/内存/磁盘）
 */
export function getSystemInfo() {
  return request.get('/system/info')
}
