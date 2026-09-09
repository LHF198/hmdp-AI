<template>
  <!-- 评论卡片：店铺评价功能尚未开放，聚合展示大家的探店笔记作为真实评价内容 -->
  <div class="shop-comments">
    <div class="comments-head">
      <div>网友评价</div>
      <span v-if="blogs.length > 0" class="comments-sub">来自探店笔记</span>
    </div>
    <div class="shop-blog-strip" v-if="blogs.length > 0">
      <div
        class="shop-blog-card"
        v-for="b in blogs"
        :key="b.id"
        @click="$emit('open', b)"
      >
        <div class="shop-blog-img">
          <img :src="b.img" alt="" loading="lazy" />
        </div>
        <div class="shop-blog-title">{{ b.title }}</div>
        <div class="shop-blog-foot">
          <img class="shop-blog-avatar" :src="b.icon || '/imgs/icons/default-icon.png'" alt="" />
          <span class="shop-blog-name">{{ b.name }}</span>
          <span class="shop-blog-liked">
            <LikeIcon :active="b.isLike" />
            {{ b.liked }}
          </span>
        </div>
      </div>
    </div>
    <EmptyState v-else size="compact" text="暂无评价，来做第一个探店笔记吧" />
  </div>
</template>

<script setup>
// 店铺评价聚合区块：横向滚动展示该店铺的探店笔记；点击跳转由父级处理
import EmptyState from '@/components/EmptyState.vue'
import LikeIcon from '@/components/LikeIcon.vue'

defineProps({
  blogs: { type: Array, default: () => [] },
})

defineEmits(['open'])
</script>
