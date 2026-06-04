<template>
  <div class="user-page">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" :inline="true" size="default">
        <el-form-item label="用户名">
          <el-input v-model="queryUsername" placeholder="模糊搜索" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="queryRealName" placeholder="模糊搜索" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryPhone" placeholder="模糊搜索" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAdd" v-if="authStore.hasRole('SUPER_ADMIN')">
          新增用户
        </el-button>
        <span class="table-tip">共 {{ total }} 条</span>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100">
          <template #default="{ row }">
            {{ row.realName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.gender === 1" type="primary" size="small">男</el-tag>
            <el-tag v-else-if="row.gender === 2" type="danger" size="small">女</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" min-width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              type="primary" link size="small" :icon="Edit"
              @click="handleEdit(row)"
              v-if="authStore.hasRole('SUPER_ADMIN') || authStore.hasRole('ADMIN')"
            >编辑</el-button>

            <el-button
              type="warning" link size="small" :icon="Key"
              @click="handleResetPassword(row)"
              v-if="authStore.hasRole('SUPER_ADMIN')"
            >重置密码</el-button>

            <el-popconfirm
              title="确定要删除该用户吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
              v-if="authStore.hasRole('SUPER_ADMIN')"
            >
              <template #reference>
                <el-button type="danger" link size="small" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="queryPage"
          :page-size="queryPageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @update:current-page="handlePageChange"
          @update:page-size="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <UserFormDialog
      v-model:visible="dialogVisible"
      :user-id="editingUserId"
      @success="handleFormSuccess"
    />

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="420px" append-to-body>
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item label="用户">
          <span>{{ resetPwdTarget?.username }} ({{ resetPwdTarget?.realName }})</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="confirmResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Search, Refresh, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getUserList, deleteUser, resetPassword } from '@/api/user'
import UserFormDialog from './UserFormDialog.vue'

const authStore = useAuthStore()

// ======================== 查询条件（不用 reactive，避免 v-model 双向绑定引发连锁反应） ========================
const queryUsername = ref('')
const queryRealName = ref('')
const queryPhone = ref('')
const queryStatus = ref(null)
const queryPage = ref(1)
const queryPageSize = ref(10)

// ======================== 表格数据 ========================
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
let fetchSeq = 0     // 请求序列号，防止并发覆盖

// ======================== 新增/编辑弹窗 ========================
const dialogVisible = ref(false)
const editingUserId = ref(null)

// ======================== 重置密码 ========================
const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdTarget = ref(null)
const resetPwdFormRef = ref(null)
const resetPwdForm = reactive({ newPassword: '' })
const resetPwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

// ======================== 获取数据（带防重入锁） ========================
let fetching = false

async function fetchData() {
  if (fetching) return    // ← 关键：防止并发重复请求
  fetching = true
  loading.value = true

  const seq = ++fetchSeq   // 记录本次请求序列号

  try {
    const params = {
      page: queryPage.value,
      pageSize: queryPageSize.value
    }
    if (queryUsername.value) params.username = queryUsername.value
    if (queryRealName.value) params.realName = queryRealName.value
    if (queryPhone.value) params.phone = queryPhone.value
    if (queryStatus.value !== null && queryStatus.value !== '') params.status = queryStatus.value

    const res = await getUserList(params)

    // 只处理最新一次请求的结果（防止旧请求覆盖新数据）
    if (seq !== fetchSeq) return

    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    if (seq !== fetchSeq) return
    tableData.value = []
    total.value = 0
  } finally {
    if (seq === fetchSeq) {
      loading.value = false
    }
    fetching = false
  }
}

// ======================== 搜索/重置 ========================
function handleSearch() {
  queryPage.value = 1
  fetchData()
}

function handleReset() {
  queryUsername.value = ''
  queryRealName.value = ''
  queryPhone.value = ''
  queryStatus.value = null
  queryPage.value = 1
  fetchData()
}

// ======================== 分页 ========================
function handlePageChange(page) {
  if (queryPage.value === page) return
  queryPage.value = page
  fetchData()
}

function handleSizeChange(size) {
  if (queryPageSize.value === size) return
  queryPageSize.value = size
  queryPage.value = 1
  fetchData()
}

// ======================== 新增/编辑 ========================
function handleAdd() {
  editingUserId.value = null
  dialogVisible.value = true
}

function handleEdit(row) {
  editingUserId.value = row.id
  dialogVisible.value = true
}

function handleFormSuccess() {
  dialogVisible.value = false
  fetchData()
}

// ======================== 删除 ========================
async function handleDelete(row) {
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 错误已在拦截器处理
  }
}

// ======================== 重置密码 ========================
function handleResetPassword(row) {
  resetPwdTarget.value = row
  resetPwdForm.newPassword = ''
  resetPwdVisible.value = true
}

async function confirmResetPassword() {
  if (!resetPwdFormRef.value) return
  try {
    await resetPwdFormRef.value.validate()
  } catch {
    return
  }

  resetPwdLoading.value = true
  try {
    await resetPassword(resetPwdTarget.value.id, resetPwdForm.newPassword)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } catch {
    // 错误已在拦截器处理
  } finally {
    resetPwdLoading.value = false
  }
}

// ======================== 初始化（nextTick 避免组件未完全挂载时触发） ========================
onMounted(() => {
  nextTick(() => {
    fetchData()
  })
})
</script>

<style scoped>
.user-page {
  max-width: 1400px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  min-height: 400px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-tip {
  font-size: 13px;
  color: #909399;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.text-muted {
  color: #c0c4cc;
}
</style>
