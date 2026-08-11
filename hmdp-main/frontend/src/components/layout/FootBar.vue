<template>
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
      <img class="add-btn" :src="addImg" alt="" />
    </div>
    <div class="foot-box" :class="{ active: activeBtn === 3 }" @click="toPage(3)" style="position: relative">
      <div class="foot-view"><el-icon :size="26"><ChatDotRound /></el-icon></div>
      <div class="foot-text">消息</div>
      <span
        v-if="hasUnread"
        style="position: absolute; top: -2px; right: 6px; width: 8px; height: 8px; border-radius: 50%; background: #f63"
      ></span>
    </div>
    <div class="foot-box" :class="{ active: activeBtn === 4 }" @click="toPage(4)">
      <div class="foot-view"><el-icon :size="26"><User /></el-icon></div>
      <div class="foot-text">我的</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '@/api/message'
import addImg from '../../../html/hmdp/imgs/add.png'

defineProps({
  activeBtn: { type: Number, default: 0 },
})

const router = useRouter()
const hasUnread = ref(false) // 消息红点：存在未读评论/关注时展示

onMounted(() => {
  // 轻量查询消息数量，未登录（401）时静默忽略
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
