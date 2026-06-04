<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑用户' : '新增用户'"
    width="560px"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      size="default"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="isEdit ? '留空则不修改' : '请输入密码'"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" placeholder="请选择" style="width: 100%">
              <el-option label="未知" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="部门ID" prop="deptId">
            <el-input-number v-model="form.deptId" :min="0" placeholder="部门ID" style="width: 100%" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="分配角色" prop="roleIds">
        <el-checkbox-group v-model="form.roleIds" v-loading="roleLoading">
          <el-checkbox
            v-for="role in roles"
            :key="role.id"
            :label="role.id"
            border
            class="role-checkbox"
          >
            {{ role.roleName }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="请输入备注信息"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '更新' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserById, createUser, updateUser } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  visible: { type: Boolean, default: false },
  userId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:visible', 'success'])
const authStore = useAuthStore()

// ======================== 状态 ========================
const formRef = ref(null)
const submitting = ref(false)
const roleLoading = ref(false)

// 是否编辑模式
const isEdit = computed(() => !!props.userId)

// 预设角色（ID对应数据库sys_role表，后续可改为从接口获取）
const roles = [
  { id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' },
  { id: 2, roleCode: 'ADMIN',       roleName: '管理员' },
  { id: 3, roleCode: 'USER',        roleName: '普通用户' },
  { id: 4, roleCode: 'AUDITOR',     roleName: '审计员' }
]

// 表单数据
const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  gender: 0,
  status: 1,
  deptId: null,
  roleIds: [],
  remark: ''
})

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名3-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能是字母、数字、下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码6-100位', trigger: 'blur' }
  ],
  realName: [
    { max: 50, message: '不超过50个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^(1[3-9]\d{9})?$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$|^$/, message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

// ======================== 监听弹窗打开 ========================
watch(() => props.visible, async (val) => {
  if (val) {
    resetForm()
    if (isEdit.value) {
      await loadUserData()
    }
  }
})

// ======================== 加载编辑数据 ========================
async function loadUserData() {
  roleLoading.value = true
  try {
    const res = await getUserById(props.userId)
    const user = res.data
    form.username = user.username || ''
    form.password = ''
    form.realName = user.realName || ''
    form.phone = user.phone || ''
    form.email = user.email || ''
    form.gender = user.gender ?? 0
    form.status = user.status ?? 1
    form.deptId = user.deptId ?? null
    form.remark = user.remark || ''

    // 加载用户角色（通过 user/me 的 roles 字段）
    // 如果后端返回了角色信息，填充到 roleIds
    if (user.roles) {
      form.roleIds = [...user.roles]
    } else {
      // 需要通过其他方式获取角色，暂时清空
      form.roleIds = []
    }
  } catch {
    ElMessage.error('获取用户信息失败')
  } finally {
    roleLoading.value = false
  }
}

// ======================== 重置表单 ========================
function resetForm() {
  form.username = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.gender = 0
  form.status = 1
  form.deptId = null
  form.roleIds = []
  form.remark = ''
  formRef.value?.clearValidate()
}

// ======================== 提交 ========================
async function handleSubmit() {
  if (!formRef.value) return

  // 表单校验
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑模式：如果密码为空，传空字符串（后端不会修改密码）
      await updateUser(props.userId, { ...form, password: form.password || '' })
      ElMessage.success('更新成功')
    } else {
      await createUser({ ...form })
      ElMessage.success('创建成功')
    }
    emit('success')
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.role-checkbox {
  margin-right: 10px;
  margin-bottom: 8px;
}
</style>
