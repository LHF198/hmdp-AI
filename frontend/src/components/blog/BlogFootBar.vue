<template>
  <!-- 底部栏：点赞 + 评论输入（Teleport 到 body：#app 的 backdrop-filter 会成为
       fixed 元素的包含块，导致底栏随内容滚动而非贴底） -->
  <Teleport to="body">
    <div class="foot blog-detail-foot">
      <!-- 点赞区 -->
      <div class="foot-like" @click="$emit('like')">
        <svg
          t="1646634642977"
          class="like-icon"
          viewBox="0 0 1024 1024"
          version="1.1"
          xmlns="http://www.w3.org/2000/svg"
          width="22"
          height="22"
        >
          <path
            d="M160 944c0 8.8-7.2 16-16 16h-32c-26.5 0-48-21.5-48-48V528c0-26.5 21.5-48 48-48h32c8.8 0 16 7.2 16 16v448zM96 416c-53 0-96 43-96 96v416c0 53 43 96 96 96h96c17.7 0 32-14.3 32-32V448c0-17.7-14.3-32-32-32H96zM505.6 64c16.2 0 26.4 8.7 31 13.9 4.6 5.2 12.1 16.3 10.3 32.4l-23.5 203.4c-4.9 42.2 8.6 84.6 36.8 116.4 28.3 31.7 68.9 49.9 111.4 49.9h271.2c6.6 0 10.8 3.3 13.2 6.1s5 7.5 4 14l-48 303.4c-6.9 43.6-29.1 83.4-62.7 112C815.8 944.2 773 960 728.9 960h-317c-33.1 0-59.9-26.8-59.9-59.9v-455c0-6.1 1.7-12 5-17.1 69.5-109 106.4-234.2 107-364h41.6z m0-64h-44.9C427.2 0 400 27.2 400 60.7c0 127.1-39.1 251.2-112 355.3v484.1c0 68.4 55.5 123.9 123.9 123.9h317c122.7 0 227.2-89.3 246.3-210.5l47.9-303.4c7.8-49.4-30.4-94.1-80.4-94.1H671.6c-50.9 0-90.5-44.4-84.6-95l23.5-203.4C617.7 55 568.7 0 505.6 0z"
            :fill="isLike ? BRAND_COLOR : '#999'"
          ></path>
        </svg>
        <span class="like-count" :class="{ liked: isLike }">{{ liked }}</span>
      </div>

      <!-- 评论输入区 -->
      <div class="comment-bar">
        <input
          class="comment-input"
          :value="modelValue"
          @input="$emit('update:modelValue', $event.target.value)"
          placeholder="说点什么，温柔一点～"
          @keyup.enter="$emit('send')"
        />
        <button class="send-btn" :class="{ active: modelValue.trim() }" @click="$emit('send')">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
// 笔记详情底部栏：点赞 + 评论输入；发送/点赞动作由父级处理
import { BRAND_COLOR } from '@/utils/colors'

defineProps({
  // 评论输入内容（v-model）
  modelValue: { type: String, default: '' },
  isLike: { type: Boolean, default: false },
  liked: { type: Number, default: 0 },
})

defineEmits(['update:modelValue', 'like', 'send'])
</script>
