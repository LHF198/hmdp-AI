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
        <div style="height: 5px"></div>
        <el-input placeholder="请输入密码" v-model="form.password"> </el-input>
        <div style="text-align: center; color: #8c939d; margin: 5px 0">
          <a href="javascript:void(0)">忘记密码</a>
        </div>
        <el-button @click="login" style="width: 100%; background-color: #f63; color: #fff">登录</el-button>
        <div style="text-align: right; color: #333333; margin: 5px 0">
          <a href="/app/login" @click.prevent="goCode">验证码登录</a>
        </div>
      </div>
      <div class="login-radio">
        <div>
          <input type="radio" name="readed" v-model="radio" value="1" />
          <label for="readed"></label>
        </div>
        <div>
          我已阅读并同意
          <a href="javascript:void(0)"> 《黑马点评用户服务协议》</a>、
          <a href="javascript:void(0)">《隐私政策》</a>
          等，接受免除或者限制责任、诉讼管辖约定等粗体标示条款
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 简化登录页：仅手机号+验证码快速登录（旧 MPA 页面 login2.html 的 SPA 迁移版，供简化入口使用）
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { loginRedirectUrl } from '@/utils/date'

const router = useRouter()
const userStore = useUserStore()

const radio = ref('')
const form = ref({})

function login() {
  if (!radio.value) {
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
