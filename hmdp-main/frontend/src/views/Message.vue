<template>
  <div>
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">消息中心&nbsp;&nbsp;&nbsp;</div>
    </div>

    <el-tabs v-model="activeName" @tab-click="handleClick">
      <el-tab-pane label="评论" name="comments">
        <div class="msg-list">
          <div v-for="c in comments" :key="c.id" class="msg-item" @click="toBlog(c.blog_id)">
            <img :src="c.user_icon || defaultIcon" alt="" />
            <div class="msg-body">
              <div class="msg-top">
                <span class="msg-nick">{{ c.user_nick_name }}</span>
                <span class="msg-time">{{ formatTime(c.create_time) }}</span>
              </div>
              <div class="msg-content">评论了你：{{ c.content }}</div>
              <div class="msg-ref">来自笔记《{{ c.blog_title }}》</div>
            </div>
          </div>
          <div v-if="!loading && comments.length === 0" class="msg-empty">
            <el-icon :size="48"><ChatDotRound /></el-icon>暂无评论消息
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="赞" name="likes">
        <div class="msg-empty">
          <el-icon :size="48"><Thumb /></el-icon>暂无点赞消息<br />
          <span style="font-size: 12px">点赞明细数据暂未开放</span>
        </div>
      </el-tab-pane>

      <el-tab-pane label="关注" name="follows">
        <div class="msg-list">
          <div v-for="f in follows" :key="f.id" class="msg-item" @click="toUser(f.user_id)">
            <img :src="f.user_icon || defaultIcon" alt="" />
            <div class="msg-body">
              <div class="msg-top">
                <span class="msg-nick">{{ f.user_nick_name }}</span>
                <span class="msg-time">{{ formatTime(f.create_time) }}</span>
              </div>
              <div class="msg-content">关注了你</div>
            </div>
          </div>
          <div v-if="!loading && follows.length === 0" class="msg-empty">
            <el-icon :size="48"><User /></el-icon>还没有人关注你
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <FootBar :active-btn="3" />
  </div>
</template>

<script setup>
// 消息中心：评论消息分页 + 关注列表（点赞明细暂未开放）；底部导航第 4 个 Tab
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api/message'
import { formatTime } from '@/utils/date'
import defaultIcon from '../../html/hmdp/imgs/icons/default-icon.png'

const router = useRouter()

const activeName = ref('comments')
const loading = ref(false)
const comments = ref([])
const follows = ref([])

onMounted(() => {
  queryComments()
  queryFollows()
})

function queryComments() {
  loading.value = true
  messageApi
    .comments()
    .then(({ data }) => {
      comments.value = data || []
    })
    .catch((err) => {
      ElMessage.error(err)
    })
    .finally(() => {
      loading.value = false
    })
}

function queryFollows() {
  messageApi
    .follows()
    .then(({ data }) => {
      follows.value = data || []
    })
    .catch((err) => {
      ElMessage.error(err)
    })
}

function handleClick() {
  // 赞 tab 暂无明细数据，直接展示空态
}

function toBlog(blogId) {
  if (blogId) {
    router.push('/blog/' + blogId)
  }
}

function toUser(userId) {
  if (userId) {
    router.push('/user/' + userId)
  }
}

function goBack() {
  router.back()
}
</script>

<style>
/* 消息列表样式（从旧 message.html 内联样式迁移，保持视觉一致） */
.msg-list {
  padding: 10px 14px;
}
.msg-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  cursor: pointer;
}
.msg-item img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #eee;
}
.msg-body {
  flex: 1;
  min-width: 0;
}
.msg-body .msg-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.msg-nick {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.msg-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}
.msg-content {
  font-size: 13px;
  color: #555;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-ref {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-empty {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 60px 0;
}
.msg-empty i {
  font-size: 48px;
  color: #ddd;
  display: block;
  margin-bottom: 12px;
}
.el-tabs__header {
  margin: 0;
  background: rgba(255, 255, 255, 0.85);
}
</style>
