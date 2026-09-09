<template>
  <!-- 评论区 -->
  <div class="blog-comments">
    <div class="comments-head">
      <div>网友评价 <span>（{{ total }}）</span></div>
    </div>
    <div class="comment-list">
      <div
        v-if="comments.length === 0"
        class="comments-empty"
      >
        暂无评价，快来抢沙发～
      </div>
      <div class="comment-box" v-for="c in comments" :key="c.id">
        <div class="comment-icon">
          <img :src="c.user_icon || defaultIcon" alt="" />
        </div>
        <div class="comment-info">
          <div class="comment-user">{{ c.user_nick_name }}</div>
          <div class="comment-content">{{ c.content }}</div>
          <div class="comment-meta">
            {{ formatCommentTime(c.create_time) }}
            <span
              v-if="userId && userId === c.user_id"
              class="comment-delete"
              @click="$emit('delete', c)"
            >删除</span>
          </div>
        </div>
      </div>
      <div
        v-if="comments.length < total"
        class="load-more"
        @click="$emit('load-more')"
      >
        加载更多评价
      </div>
    </div>
  </div>
</template>

<script setup>
// 笔记评论区：列表 + 分页加载更多 + 作者删除入口（删除/加载动作由父级处理）
import defaultIcon from '../../../html/hmdp/imgs/icons/default-icon.png'

defineProps({
  comments: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  // 登录用户 id（未登录为 undefined），用于判断是否展示删除入口
  userId: { type: Number, default: undefined },
})

defineEmits(['load-more', 'delete'])

function formatCommentTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return (
    d.getMonth() + 1 + '月' + d.getDate() + '日 ' +
    d.getHours() + ':' + (d.getMinutes() < 10 ? '0' + d.getMinutes() : d.getMinutes())
  )
}
</script>
