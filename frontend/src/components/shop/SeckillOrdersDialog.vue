<template>
  <!-- 我的秒杀订单弹窗 -->
  <el-dialog
    title="我的秒杀订单"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    width="90%"
    append-to-body
  >
    <EmptyState v-if="orders.length === 0" size="compact" text="暂无订单" />
    <div
      v-for="o in orders"
      :key="o.id"
      class="order-item"
    >
      <div class="order-title">
        {{ o.voucherTitle || '代金券 ' + o.voucherId }}
      </div>
      <div class="order-meta">
        订单号：{{ o.id }}
        <span v-if="o.payValue"> | ￥{{ formatPrice(o.payValue) }}</span>
        | {{ orderStatus(o.status) }}
      </div>
      <div class="order-time">
        下单时间：{{ formatTime(o.createTime) }}
      </div>
      <div class="order-actions">
        <el-button
          v-if="o.status === ORDER_STATUS.UNPAID"
          size="small"
          type="danger"
          round
          @click="$emit('pay', o)"
        >模拟支付</el-button>
        <span
          v-if="o.status === ORDER_STATUS.PAID || o.status === ORDER_STATUS.USED"
          class="order-code"
        >核销码：{{ o.id }}</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
// 我的秒杀订单弹窗：纯展示 + 模拟支付入口（支付动作由父级处理）
import EmptyState from '@/components/EmptyState.vue'
import { ORDER_STATUS, ORDER_STATUS_TEXT } from '@/utils/order-status'
import { formatPrice } from '@/utils/format'

defineProps({
  // 弹窗开关（v-model）
  modelValue: { type: Boolean, default: false },
  orders: { type: Array, default: () => [] },
})

defineEmits(['update:modelValue', 'pay'])

function orderStatus(s) {
  return ORDER_STATUS_TEXT[s] || '未知'
}

function formatTime(t) {
  // 兼容 LocalDateTime 数组 [y,m,d,h,mi,s] 与字符串
  if (!t) return ''
  if (Array.isArray(t)) {
    const p = (n) => (n < 10 ? '0' + n : '' + n)
    return `${t[0]}-${p(t[1])}-${p(t[2])} ${p(t[3] || 0)}:${p(t[4] || 0)}`
  }
  return String(t).replace('T', ' ').split('.')[0].substring(0, 16)
}
</script>
