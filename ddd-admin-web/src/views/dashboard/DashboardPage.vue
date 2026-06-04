<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card">
          <div class="welcome-content">
            <div class="welcome-text">
              <h2>👋 欢迎回来，{{ authStore.realName }}</h2>
              <p>当前角色：{{ authStore.roles.join(' | ') || '未分配' }} &nbsp;|&nbsp; OS：{{ osName }}</p>
              <p class="welcome-time">{{ currentTime }}</p>
            </div>
            <div class="welcome-avatar">
              <el-avatar :size="72" :icon="UserFilled" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- CPU + GPU -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="hw-card">
          <template #header>
            <div class="card-header">
              <span><el-icon color="#409eff"><Cpu /></el-icon> CPU</span>
              <el-tag :type="cpuUsage > 80 ? 'danger' : cpuUsage > 50 ? 'warning' : 'success'" size="small">
                {{ cpuUsage }}%
              </el-tag>
            </div>
          </template>
          <div class="hw-body">
            <div class="hw-name">{{ cpuName }}</div>
            <div class="hw-detail">厂商: {{ cpuVendor }} &nbsp;|&nbsp; {{ cpuCores }}核({{ cpuLogical }}线程)</div>
            <div class="hw-detail">最大频率: {{ cpuFreq }}</div>
            <div class="progress-wrapper">
              <el-progress :percentage="cpuUsage" :color="cpuColor" :stroke-width="16" :text-inside="true" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="hw-card">
          <template #header>
            <div class="card-header">
              <span><el-icon color="#67c23a"><Monitor /></el-icon> GPU</span>
              <el-tag v-if="gpuUsage !== 'N/A'" :type="parseFloat(gpuUsage) > 80 ? 'danger' : parseFloat(gpuUsage) > 50 ? 'warning' : 'success'" size="small">
                {{ gpuUsage }}
              </el-tag>
              <el-tag v-else type="info" size="small">N/A</el-tag>
            </div>
          </template>
          <div class="hw-body" v-for="(gpu, i) in gpuList" :key="i">
            <div class="hw-name">{{ gpu.name }}</div>
            <div class="hw-detail">显存: {{ gpu.vram }} &nbsp;|&nbsp; 温度: {{ gpu.temperature }}</div>
            <div class="progress-wrapper" v-if="gpu.usage !== 'N/A'">
              <el-progress :percentage="parseFloat(gpu.usage)" :color="gpuColor" :stroke-width="16" :text-inside="true" />
            </div>
            <div v-else class="hw-detail muted">实时使用率不可用（需 NVIDIA 显卡 + nvidia-smi）</div>
            <el-divider v-if="i < gpuList.length - 1" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 内存 + 磁盘 -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="hw-card">
          <template #header>
            <div class="card-header">
              <span><el-icon color="#e6a23c"><Coin /></el-icon> 内存</span>
              <el-tag :type="memUsage > 85 ? 'danger' : memUsage > 60 ? 'warning' : 'success'" size="small">
                {{ memUsage }}%
              </el-tag>
            </div>
          </template>
          <div class="hw-body">
            <div class="hw-stats">
              <div class="stat-item">
                <span class="stat-label">总量</span>
                <span class="stat-val">{{ memTotal }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已用</span>
                <span class="stat-val warn">{{ memUsed }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">可用</span>
                <span class="stat-val success">{{ memAvailable }}</span>
              </div>
            </div>
            <el-progress :percentage="memUsage" :color="memColor" :stroke-width="20" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="hw-card">
          <template #header>
            <div class="card-header">
              <span><el-icon color="#909399"><Files /></el-icon> 磁盘</span>
              <el-tag :type="diskUsage > 85 ? 'danger' : diskUsage > 60 ? 'warning' : 'success'" size="small">
                {{ diskUsage }}%
              </el-tag>
            </div>
          </template>
          <div class="hw-body">
            <div class="hw-stats">
              <div class="stat-item">
                <span class="stat-label">总量</span>
                <span class="stat-val">{{ diskTotal }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已用</span>
                <span class="stat-val warn">{{ diskUsed }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">可用</span>
                <span class="stat-val success">{{ diskFree }}</span>
              </div>
            </div>
            <el-progress :percentage="diskUsage" :color="diskColor" :stroke-width="20" />
            <div class="disk-list">
              <div v-for="d in diskList" :key="d.mount" class="disk-row">
                <span class="disk-mount">{{ d.mount }}</span>
                <span class="disk-info">{{ d.total }} / {{ d.free }} 可用</span>
                <span class="disk-pct" :class="{ 'text-danger': d.usage > 85 }">{{ d.usage }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { UserFilled, Cpu, Monitor, Coin, Files } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getSystemInfo } from '@/api/system'

const authStore = useAuthStore()

// ======================== 硬件信息 ========================
const cpuName = ref('加载中...')
const cpuVendor = ref('')
const cpuCores = ref(0)
const cpuLogical = ref(0)
const cpuFreq = ref('')
const cpuUsage = ref(0)
const gpuList = ref([])
const gpuUsage = ref('N/A')
const memTotal = ref('')
const memUsed = ref('')
const memAvailable = ref('')
const memUsage = ref(0)
const diskTotal = ref('')
const diskUsed = ref('')
const diskFree = ref('')
const diskUsage = ref(0)
const diskList = ref([])
const osName = ref('')

// ======================== 时间 ========================
const currentTime = ref('')
let timer = null
let hwTimer = null

function updateTime() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  currentTime.value = `${now.getFullYear()}年${now.getMonth()+1}月${now.getDate()}日 ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

// ======================== 获取硬件信息 ========================
async function fetchHardwareInfo() {
  try {
    const res = await getSystemInfo()
    const d = res.data

    // CPU
    cpuName.value = d.cpu.name
    cpuVendor.value = d.cpu.vendor
    cpuCores.value = d.cpu.physicalCores
    cpuLogical.value = d.cpu.logicalCores
    cpuFreq.value = d.cpu.maxFreq
    cpuUsage.value = d.cpu.usage

    // GPU
    gpuList.value = d.gpu
    gpuUsage.value = d.gpu[0]?.usage || 'N/A'

    // 内存
    memTotal.value = d.memory.total
    memUsed.value = d.memory.used
    memAvailable.value = d.memory.available
    memUsage.value = d.memory.usage

    // 磁盘
    diskTotal.value = d.disk.totalAll
    diskFree.value = d.disk.freeAll
    diskUsed.value = formatDiskUsed(d.disk.totalAll, d.disk.freeAll)
    diskUsage.value = d.disk.usageAll
    diskList.value = d.disk.disks || []

    // OS
    osName.value = d.os?.name || ''
  } catch {
    // 静默处理，界面保持上一次的值
  }
}

function formatDiskUsed(total, free) {
  if (!total || !free) return ''
  // 简单解析：total 如 "476.94 GB"，free 同理
  const totalNum = parseFloat(total)
  const freeNum = parseFloat(free)
  if (isNaN(totalNum) || isNaN(freeNum)) return ''
  const unit = total.replace(/[\d.]+/, '').trim()
  return (totalNum - freeNum).toFixed(2) + ' ' + unit
}

// ======================== 进度条颜色 ========================
const cpuColor = computed(() => cpuUsage.value > 80 ? '#f56c6c' : cpuUsage.value > 50 ? '#e6a23c' : '#67c23a')
const gpuColor = computed(() => {
  const u = parseFloat(gpuUsage.value)
  return u > 80 ? '#f56c6c' : u > 50 ? '#e6a23c' : '#67c23a'
})
const memColor = computed(() => memUsage.value > 85 ? '#f56c6c' : memUsage.value > 60 ? '#e6a23c' : '#67c23a')
const diskColor = computed(() => diskUsage.value > 85 ? '#f56c6c' : diskUsage.value > 60 ? '#e6a23c' : '#67c23a')

// ======================== 生命周期 ========================
onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  fetchHardwareInfo()
  hwTimer = setInterval(fetchHardwareInfo, 3000)  // 每3秒刷新
})

onUnmounted(() => {
  clearInterval(timer)
  clearInterval(hwTimer)
})
</script>

<style scoped>
.dashboard { max-width: 1400px; }

/* 欢迎卡片 */
.welcome-card { margin-bottom: 20px; }
.welcome-content { display: flex; justify-content: space-between; align-items: center; }
.welcome-text h2 { margin-bottom: 8px; font-size: 22px; color: #303133; }
.welcome-text p { color: #909399; font-size: 14px; margin-bottom: 4px; }
.welcome-time { font-size: 13px !important; color: #c0c4cc !important; }

/* 硬件卡片 */
.hw-card { margin-bottom: 20px; min-height: 220px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header span { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.hw-body { padding: 0 4px; }
.hw-name { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.hw-detail { font-size: 13px; color: #909399; margin-bottom: 4px; }
.hw-detail.muted { font-style: italic; color: #c0c4cc; margin-top: 8px; }
.progress-wrapper { margin-top: 16px; }

/* 统计数值 */
.hw-stats { display: flex; justify-content: space-around; margin-bottom: 16px; text-align: center; }
.stat-item { }
.stat-label { display: block; font-size: 12px; color: #c0c4cc; margin-bottom: 4px; }
.stat-val { font-size: 18px; font-weight: 700; color: #303133; }
.stat-val.warn { color: #e6a23c; }
.stat-val.success { color: #67c23a; }

/* 磁盘列表 */
.disk-list { margin-top: 16px; border-top: 1px solid #ebeef5; padding-top: 12px; }
.disk-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 13px; }
.disk-mount { font-weight: 600; color: #303133; width: 80px; }
.disk-info { color: #909399; flex: 1; }
.disk-pct { color: #67c23a; font-weight: 600; margin-left: 12px; }
.disk-pct.text-danger { color: #f56c6c; }
</style>
