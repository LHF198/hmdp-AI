<template>
  <!-- Teleport 到 body：绕开 #app 的 backdrop-filter 包含块陷阱。
       layout.css 的 #app 带 backdrop-filter，会把内部 position:fixed 元素的定位参照
       从视口劫持为 #app（内容超过一屏时底栏会被推到页面底部，完全不可见）。
       Teleport 后 .foot 相对视口固定；其 width:calc(100%-24px)+居中样式与 #app 恰好对齐。 -->
  <Teleport to="body">
    <div class="foot">
    <div class="foot-box" :class="{ active: activeBtn === 1 }" @click="toPage(1)">
      <div class="foot-view"><el-icon :size="26"><HomeFilled /></el-icon></div>
      <div class="foot-text">首页</div>
    </div>
    <div class="foot-box" :class="{ active: activeBtn === 2 }" @click="toPage(2)">
      <div class="foot-view"><el-icon :size="26"><MapLocation /></el-icon></div>
      <div class="foot-text">地图</div>
    </div>
    <div class="foot-box" @click="toPage(0)">
      <!-- 发布按钮：品牌橙凸起胶囊（替代旧黑色圆图，样式见 layout.css .foot-add） -->
      <div class="foot-add"><el-icon :size="22"><Plus /></el-icon></div>
    </div>
    <div class="foot-box" :class="{ active: activeBtn === 3 }" @click="toPage(3)">
      <div class="foot-view"><el-icon :size="26"><ChatDotRound /></el-icon></div>
      <div class="foot-text">消息</div>
      <!-- 未读红点：定位与配色见 styles/layout.css .foot-badge -->
      <span v-if="hasUnread" class="foot-badge" aria-label="有未读消息"></span>
    </div>
    <div class="foot-box" :class="{ active: activeBtn === 4 }" @click="toPage(4)">
      <div class="foot-view"><el-icon :size="26"><User /></el-icon></div>
      <div class="foot-text">我的</div>
    </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '@/api/message'

defineProps({
  activeBtn: { type: Number, default: 0 },
})

const router = useRouter()
const hasUnread = ref(false) // 消息红点：存在未读评论/关注时展示

onMounted(() => {
  // 轻量查询消息数量。未登录（无 token）时直接跳过，避免 401 触发
  // http.js 的全局跳登录拦截，把整个页面劫持到登录页（曾导致未登录访问首页被重定向）
  if (!sessionStorage.getItem('token')) return
  messageApi
    .comments()
    .then(({ data }) => {
      if (data && data.length > 0) {
        hasUnread.value = true
      }
    })
    .catch(() => {})
})

// 与旧 footer.js 一致的 tab 路由映射
const pageMap = {
  0: '/blog/edit', // 发布（中间 + 号）
  1: '/', // 首页
  2: '/map', // 地图
  3: '/message', // 消息
  4: '/profile', // 我的
}

function toPage(i) {
  router.push(pageMap[i])
}
</script>
