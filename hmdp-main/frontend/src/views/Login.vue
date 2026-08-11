<template>
  <div class="login-container">
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">手机号码快捷登录&nbsp;&nbsp;&nbsp;</div>
    </div>
    <div class="content">
      <div class="login-form">
        <div style="display: flex; justify-content: space-between">
          <el-input style="width: 60%" placeholder="请输入手机号" v-model="form.phone"> </el-input>
          <el-button style="width: 38%" @click="sendCode" type="success" :disabled="disabled">{{ codeBtnMsg }}</el-button>
        </div>

        <div style="height: 5px"></div>
        <el-input placeholder="请输入验证码" v-model="form.code"> </el-input>
        <div style="text-align: center; color: #8c939d; margin: 5px 0">未注册的手机号码验证后自动创建账户</div>
        <el-button @click="login" style="width: 100%; background-color: rgb(255, 255, 255); color: #fff !important">
          登录
        </el-button>
        <div style="text-align: right; color: #333333; margin: 5px 0">
          <a href="/app/login2" @click.prevent="goPassword">密码登录</a>
        </div>
      </div>
      <div class="login-radio">
        <div>
          <input id="agree" type="radio" name="readed" v-model="radio" value="1" />
          <label for="agree"></label>
        </div>
        <div>
          我已阅读并同意
          <a href="javascript:void(0)"> 《AI点评用户服务协议》</a>、
          <a href="javascript:void(0)">《隐私政策》</a>
          等，接受免除或者限制责任、诉讼管辖约定等粗体标示条款
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 登录页：手机号+验证码登录（未注册自动注册），需勾选用户协议；登录成功回跳来源页
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { loginRedirectUrl } from '@/utils/date'

const router = useRouter()
const userStore = useUserStore()

const radio = ref('')
const disabled = ref(false) // 发送短信按钮
const codeBtnMsg = ref('发送验证码') // 发送短信按钮提示
const form = ref({})

let countdownTask = null // 倒计时定时器（组件卸载时清理）

function login() {
  if (!radio.value) {
    ElMessage.error('请先确认阅读用户协议！')
    return
  }
  if (!form.value.phone || !form.value.code) {
    ElMessage.error('手机号和验证码不能为空！')
    return
  }
  userApi
    .login(form.value)
    .then(({ data }) => {
      if (data) {
        // 保存用户信息到 session（与旧页面共用存储）
        userStore.setToken(data)
      }
      // 返回触发登录的来源页（无来源页则回首页）
      location.href = loginRedirectUrl()
    })
    .catch((err) => ElMessage.error(err))
}

function goBack() {
  router.back()
}

function goPassword() {
  router.push('/login2')
}

function sendCode() {
  if (!form.value.phone) {
    ElMessage.error('手机号不能为空')
    return
  }
  // 发送验证码（演示环境直接回显验证码，免翻后端日志）
  userApi
    .sendCode(form.value.phone)
    .then(({ data }) => {
      ElMessage.success('验证码已发送：' + data + '（演示环境）')
    })
    .catch((err) => {
      console.log(err)
      ElMessage.error(err)
    })
  // 禁用按钮
  disabled.value = true
  // 按钮倒计时
  let i = 60
  codeBtnMsg.value = i-- + '秒后可重发'
  countdownTask = setInterval(() => (codeBtnMsg.value = i-- + '秒后可重发'), 1000)
  setTimeout(() => {
    disabled.value = false
    clearInterval(countdownTask)
    codeBtnMsg.value = '发送验证码'
  }, 59000)
}

onUnmounted(() => {
  // 离开页面时清理倒计时，避免泄漏
  if (countdownTask) clearInterval(countdownTask)
})
</script>
