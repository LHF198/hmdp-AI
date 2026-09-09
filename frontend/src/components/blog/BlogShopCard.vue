<template>
  <!-- 关联店铺卡片 -->
  <div class="shop-basic" @click="$emit('open')" title="点击查看商铺详情" v-if="shop.id">
    <div class="shop-icon">
      <img :src="shop.image" alt="" />
    </div>
    <div class="shop-card-info">
      <div class="name">{{ shop.name }}</div>
      <div>
        <el-rate disabled :model-value="shop.score / 10"> </el-rate>
      </div>
      <div class="shop-avg">￥{{ shop.avgPrice }}/人</div>
    </div>
  </div>
</template>

<script setup>
// 笔记内嵌的关联店铺卡片：点击由父级跳转商铺详情页
defineProps({
  shop: { type: Object, default: () => ({}) },
})

defineEmits(['open'])
</script>
