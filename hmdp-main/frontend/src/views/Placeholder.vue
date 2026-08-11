<template>
  <div class="placeholder-page">
    <div class="placeholder-card">
      <el-icon :size="52" color="#c0c4cc"><Box /></el-icon>
      <p class="title">{{ title }}</p>
      <p class="tip">该页面将在后续迁移阶段上线</p>
      <el-button type="primary" plain @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
// 占位页：为尚未迁移的旧 MPA 路由提供临时壳，展示“后续迁移阶段上线”提示，避免 404
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const title = route.meta.title || '页面'
</script>

<style scoped>
.placeholder-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef1f5 100%);
}
.placeholder-card {
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 40px 48px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}
.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 12px 0 4px;
}
.tip {
  font-size: 13px;
  color: #909399;
  margin: 0 0 20px;
}
</style>
