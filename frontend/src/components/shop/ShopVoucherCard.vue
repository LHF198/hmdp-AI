<template>
  <!-- 代金券卡片 -->
  <div class="shop-voucher" v-if="vouchers.length > 0">
    <div class="voucher-header">
      <span class="voucher-icon">券</span>
      <span class="voucher-title-text">代金券</span>
    </div>
    <!-- 未到结束时间的代金券列表（Vue 3 中 v-if 优先于 v-for，不能同元素使用，提前 computed 过滤） -->
    <div class="voucher-box" v-for="v in activeVouchers" :key="v.id">
      <div class="voucher-circle">
        <div class="voucher-b"></div>
        <div class="voucher-b"></div>
        <div class="voucher-b"></div>
      </div>
      <div class="voucher-left">
        <div class="voucher-title">{{ v.title }}</div>
        <div class="voucher-subtitle">{{ v.subTitle }}</div>
        <div class="voucher-price">
          <div>￥{{ formatPrice(v.payValue) }}</div>
          <span>{{ ((v.payValue * 10) / v.actualValue).toFixed(1) }}折</span>
        </div>
      </div>
      <div class="voucher-right">
        <!-- 秒杀类型代金券 -->
        <div v-if="v.type" class="seckill-box">
          <div
            class="voucher-btn"
            :class="{ 'disable-btn': isNotBegin(v) || v.stock < 1 || seckilling }"
            @click="$emit('seckill', v)"
          >
            {{ seckilling ? '抢购中...' : '限时抢购' }}
          </div>
          <div class="seckill-stock">剩余 <span>{{ v.stock }}</span> 张</div>
          <div class="seckill-time">{{ countdownText(v) }}</div>
        </div>
        <!-- 普通代金券 -->
        <div class="voucher-btn" v-else>抢购</div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 代金券/秒杀区块：列表过滤、倒计时与禁用态在本组件内计算；秒杀下单动作由父级处理
import { computed } from 'vue'
import { formatPrice } from '@/utils/format'

const props = defineProps({
  vouchers: { type: Array, default: () => [] },
  // 秒杀请求在途标记：防快速连点重复下单
  seckilling: { type: Boolean, default: false },
  // 当前时间戳（父级每秒刷新），驱动秒杀倒计时
  nowTs: { type: Number, default: 0 },
})

defineEmits(['seckill'])

// 代金券列表（过滤已结束的秒杀券；isEnd 内部依赖 nowTs，computed 会随倒计时刷新）
const activeVouchers = computed(() => props.vouchers.filter((v) => !isEnd(v)))

function isNotBegin(v) {
  return new Date(v.beginTime).getTime() > props.nowTs
}

function isEnd(v) {
  return new Date(v.endTime).getTime() < props.nowTs
}

function countdownText(v) {
  // 秒杀倒计时：未开始显示距开始，进行中显示距结束
  const begin = new Date(v.beginTime).getTime()
  const end = new Date(v.endTime).getTime()
  if (props.nowTs < begin) {
    return '距开始 ' + countdownHms(begin - props.nowTs)
  }
  if (props.nowTs < end) {
    return '距结束 ' + countdownHms(end - props.nowTs)
  }
  const b = new Date(v.beginTime)
  const e = new Date(v.endTime)
  return (
    b.getMonth() + 1 + '月' + b.getDate() + '日 ' + b.getHours() + ':' + formatMinutes(b.getMinutes()) +
    ' ~ ' + e.getHours() + ':' + formatMinutes(e.getMinutes())
  )
}

function countdownHms(ms) {
  let s = Math.max(0, Math.floor(ms / 1000))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return pad(h) + ':' + pad(m) + ':' + pad(sec)
}

function formatMinutes(m) {
  if (m < 10) m = '0' + m
  return m
}
</script>
