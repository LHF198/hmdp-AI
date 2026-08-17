<template>
  <div class="login-container">
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">密码登录&nbsp;&nbsp;&nbsp;</div>
    </div>
    <div class="content">
      <div class="login-form">
        <el-input placeholder="请输入手机号" v-model="form.phone"> </el-input>
        <el-input placeholder="请输入密码" v-model="form.password" type="password" show-password> </el-input>
        <div class="form-tip">
          <a href="javascript:void(0)" @click="goCode">忘记密码？用验证码登录</a>
        </div>
        <el-button type="primary" class="login-btn" @click="login">登录</el-button>
        <div class="form-right">
          <a href="/app/login" @click.prevent="goCode">验证码登录</a>
        </div>
      </div>
      <div class="login-radio">
        <el-checkbox v-model="agreed" size="small" class="agree-checkbox">
          <span class="agree-text">我已阅读并同意</span>
          <a href="javascript:void(0)">《黑马点评用户服务协议》</a>、
          <a href="javascript:void(0)">《隐私政策》</a>
          <span class="agree-text">等，接受免除或者限制责任、诉讼管辖约定等粗体标示条款</span>
        </el-checkbox>
      </div>
    </div>
  </div>
</template>

<script setup>
// 密码登录页（旧 MPA 页面 login2.html 的 SPA 迁移版）：手机号 + 密码登录，
// 密码通过「我的-修改密码」或注册后设置；未设置密码的账号会提示先验证码登录
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { loginRedirectUrl } from '@/utils/date'

const router = useRouter()
const userStore = useUserStore()

const agreed = ref(false)
const form = ref({})

function login() {
  if (!agreed.value) {
    ElMessage.error('请先确认阅读用户协议！')
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

function goCode() {
  router.push('/login')
}
</script>

<style scoped>
/* 表单通用样式见 styles/page-login.css；此处仅协议勾选框的换行处理 */
.agree-checkbox {
  width: 100%;
  white-space: normal;
  line-height: 1.5;
}
.agree-text {
  display: inline;
}
</style>
