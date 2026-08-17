<template>
  <!-- 统一空态：替代此前散落在 6 处页面模板里的重复内联样式 -->
  <div :class="['empty-state', sizeClass]">
    <slot>{{ text }}</slot>
  </div>
</template>

<script setup>
// 空态占位文案组件：样式由 styles/components.css 的 .empty-state 提供
import { computed } from 'vue'

const props = defineProps({
  text: { type: String, default: '暂无内容' },
  // compact: 弹窗/卡片内；default: Tab 内容区；roomy: 整页列表
  size: { type: String, default: 'default' },
})

const sizeClass = computed(() => {
  if (props.size === 'compact') return 'empty-state--compact'
  if (props.size === 'roomy') return 'empty-state--roomy'
  return ''
})
</script>
