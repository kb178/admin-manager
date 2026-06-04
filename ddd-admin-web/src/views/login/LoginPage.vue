<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 标题 -->
      <div class="login-header">
        <h1 class="login-title">DDD Admin</h1>
        <p class="login-subtitle">企业级后台管理系统</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <!-- 验证码 -->
        <el-form-item prop="captcha">
          <div class="captcha-row">
            <el-input
              v-model="form.captcha"
              placeholder="验证码"
              :prefix-icon="Key"
              maxlength="4"
              class="captcha-input"
            />
            <div class="captcha-img-wrapper" @click="refreshCaptcha" title="点击刷新验证码">
              <img
                v-if="captchaImage"
                :src="captchaImage"
                alt="验证码"
                class="captcha-img"
              />
              <el-icon v-else class="captcha-loading"><Loading /></el-icon>
            </div>
          </div>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部版权 -->
    <p class="login-footer">Copyright © 2026 DDD Admin</p>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, Key, Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 表单引用
const formRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')

// 表单数据
const form = reactive({
  username: '',
  password: '',
  captcha: ''
})

// 校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, max: 100, message: '密码长度至少 3 位', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为 4 位', trigger: 'blur' }
  ]
}

// 获取验证码
async function refreshCaptcha() {
  try {
    const data = await authStore.fetchCaptcha()
    captchaImage.value = data.captchaImage
  } catch {
    // 错误已在拦截器中处理
  }
}

// 登录
async function handleLogin() {
  if (!formRef.value) return

  // 表单校验
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login(form)
    // 登录成功 → 跳转
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch {
    // 登录失败 → 刷新验证码
    refreshCaptcha()
    form.captcha = ''
  } finally {
    loading.value = false
  }
}

// 初始化：加载验证码
onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 2px;
}

.login-subtitle {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img-wrapper {
  width: 130px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.captcha-img-wrapper:hover {
  border-color: #409eff;
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  font-size: 20px;
  color: #c0c4cc;
}

.login-btn {
  width: 100%;
  letter-spacing: 4px;
  font-size: 16px;
}

.login-footer {
  margin-top: 24px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}
</style>
