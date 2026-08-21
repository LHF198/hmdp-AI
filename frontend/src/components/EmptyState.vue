<template>
  <!-- 统一空态：替代此前散落在 6 处页面模板里的重复内联样式 -->
  <div :class="['empty-state', sizeClass]">
    <!-- 空态图标：提升空态视觉辨识度 -->
    <div class="empty-state-icon" v-if="props.icon">
      <el-icon :size="36" color="var(--text-muted)"><component :is="props.icon" /></el-icon>
    </div>
    <slot>{{ text }}</slot>
    <!-- CTA 按钮：引导用户行动，提升用户留存 -->
    <div v-if="actionText" class="empty-state-action" @click="$emit('action')">
      {{ actionText }}
    </div>
  </div>
</template>

<script setup>
// 空态占位文案组件：样式由 styles/components.css 的 .empty-state 提供
import { computed } from 'vue'

const props = defineProps({
  text: { type: String, default: '暂无内容' },
  // compact: 弹窗/卡片内；default: Tab 内容区；roomy: 整页列表
  size: { type: String, default: 'default' },
  // CTA 按钮文案，不传则不显示按钮
  actionText: { type: String, default: '' },
  // 空态图标组件名（可选）
  icon: { type: [Object, String], default: null },
})

defineEmits(['action'])

const sizeClass = computed(() => {
  if (props.size === 'compact') return 'empty-state--compact'
  if (props.size === 'roomy') return 'empty-state--roomy'
  return ''
})
</script>
