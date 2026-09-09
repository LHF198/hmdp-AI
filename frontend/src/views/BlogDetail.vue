<template>
  <div class="blog-detail-page">
    <!-- 页面头部：返回 / 标题 / 删除（作者）或分享 -->
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">{{ blog.title }}</div>
      <div
        class="header-delete"
        v-if="user && blog.userId && user.id === blog.userId"
        @click="deleteBlog"
      >
        删除
      </div>
      <div class="header-share" v-else title="复制笔记链接分享" @click="share">...</div>
    </div>

    <!-- 笔记不存在/加载失败时的错误空态（避免渲染 NaN 日期、空头像等残缺假页面） -->
    <div class="detail-body" v-if="loadError">
      <EmptyState size="roomy">
        <div class="blog-missing">
          <div class="blog-missing-text">笔记不存在或已删除</div>
          <div class="blog-missing-btn" @click="router.push('/')">返回首页</div>
        </div>
      </EmptyState>
    </div>

    <div class="detail-body" v-else>
      <!-- 图片轮播（触摸滑动 + 鼠标拖拽） -->
      <BlogSwiper :images="blog.images" />

      <!-- 作者信息栏 -->
      <div class="basic">
        <div class="basic-icon" @click="toOtherInfo">
          <img :src="blog.icon || defaultIcon" alt="" />
        </div>
        <div class="basic-info">
          <div class="name">{{ blog.name }}</div>
          <span class="time">{{ formatTime(new Date(blog.createTime)) }}</span>
        </div>
        <div class="basic-follow-col">
          <div
            class="logout-btn"
            @click="follow"
            v-show="!user || user.id !== blog.userId"
          >
            {{ followed ? '取消关注' : '关注' }}
          </div>
        </div>
      </div>

      <!-- 笔记标题：完整展示（header 中间为单行省略版） -->
      <div class="blog-detail-title" v-if="blog.title">{{ blog.title }}</div>

      <!-- 笔记正文：插值渲染（v-html 渲染用户输入存在存储型 XSS 风险，已移除；pre-wrap 保留换行） -->
      <div class="blog-text">{{ blog.content }}</div>

      <!-- 关联店铺卡片 -->
      <BlogShopCard :shop="shop" @open="toShopDetail" />

      <!-- 点赞区 -->
      <BlogLikes :likes="likes" :liked="blog.liked" :is-like="blog.isLike" />

      <div class="blog-divider"></div>

      <!-- 评论区 -->
      <BlogComments
        :comments="comments"
        :total="commentTotal"
        :user-id="user.id"
        @load-more="loadMoreComments"
        @delete="deleteComment"
      />
      <div class="blog-divider"></div>
    </div>

    <!-- 底部栏：点赞 + 评论输入（组件内 Teleport 到 body，见 BlogFootBar） -->
    <BlogFootBar
      v-if="!loadError"
      v-model="commentContent"
      :is-like="blog.isLike"
      :liked="blog.liked"
      @like="addLike"
      @send="sendComment"
    />
  </div>
</template>

<script setup>
// 笔记详情页：展示笔记图文/作者/关联店铺卡片，支持点赞、评论、关注与分享；
// 路由参数 blogId 驱动数据加载（旧 MPA 页面 blog-detail.html 的 SPA 迁移版）
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { blogApi, commentApi } from '@/api/blog'
import { shopApi } from '@/api/shop'
import { userApi } from '@/api/user'
import { followApi } from '@/api/follow'
import { useUserStore } from '@/stores/user'
import BlogSwiper from '@/components/blog/BlogSwiper.vue'
import BlogShopCard from '@/components/blog/BlogShopCard.vue'
import BlogLikes from '@/components/blog/BlogLikes.vue'
import BlogComments from '@/components/blog/BlogComments.vue'
import BlogFootBar from '@/components/blog/BlogFootBar.vue'
import defaultIcon from '../../html/hmdp/imgs/icons/default-icon.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const blog = ref({})
const shop = ref({})
const likes = ref([])
const loadError = ref(false) // 笔记加载失败（不存在/已删除）时切换到错误空态
const user = ref({}) // 登录用户
const followed = ref(false) // 是否关注了
const comments = ref([]) // 评论列表
const commentTotal = ref(0) // 评论总数
const commentPage = ref(1) // 评论当前页
const commentContent = ref('') // 评论输入内容

onMounted(() => {
  queryBlogById(route.params.id)
  queryComments()
})

function goBack() {
  router.back()
}

function share() {
  // 复制当前笔记链接到剪贴板，便于分享给好友
  const url = location.href
  const notify = () => ElMessage.success('笔记链接已复制，快去分享吧')
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard
      .writeText(url)
      .then(notify)
      .catch(() => shareFallback(url, notify))
  } else {
    shareFallback(url, notify)
  }
}

function shareFallback(url, notify) {
  const ta = document.createElement('textarea')
  ta.value = url
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  let ok = false
  try {
    ok = document.execCommand('copy')
  } catch (e) {}
  document.body.removeChild(ta)
  if (ok) {
    notify()
  } else {
    ElMessage.info(url)
  }
}

function toOtherInfo() {
  // 未登录时 user.value.id 为 undefined，不能与 blog.userId 相等判断
  if (user.value.id && blog.value.userId === user.value.id) {
    router.push('/profile')
  } else {
    router.push('/user/' + blog.value.userId)
  }
}

function queryBlogById(id) {
  blogApi
    .get(id)
    .then(({ data }) => {
      data.images = data.images ? data.images.split(',') : []
      blog.value = data
      if (data.shopId) {
        queryShopById(data.shopId)
      }
      queryLikeList(id)
      queryLoginUser()
    })
    .catch((err) => {
      loadError.value = true
      ElMessage.error(err)
    })
}

function queryShopById(shopId) {
  shopApi
    .detail(shopId)
    .then(({ data }) => {
      data.image = (data.images || '').split(',')[0]
      shop.value = data
    })
    .catch((err) => ElMessage.error(err))
}

function toShopDetail() {
  // 点击笔记内的商铺卡片，跳转商铺详情页
  if (shop.value && shop.value.id) {
    router.push('/shop/' + shop.value.id)
  }
}

function queryLikeList(id) {
  blogApi
    .likes(id)
    .then(({ data }) => (likes.value = data))
    .catch((err) => ElMessage.error(err))
}

function queryComments() {
  // 分页查询笔记评论（含评论人昵称/头像）
  commentApi
    .list(route.params.id, commentPage.value)
    .then(({ data }) => {
      if (commentPage.value === 1) {
        comments.value = data.records
      } else {
        comments.value = comments.value.concat(data.records)
      }
      commentTotal.value = data.total
    })
    .catch(() => {})
}

function loadMoreComments() {
  commentPage.value++
  queryComments()
}

function sendComment() {
  if (!userStore.isLoggedIn) {
    ElMessage.error('请先登录')
    setTimeout(() => {
      sessionStorage.setItem('login_from', location.pathname + location.search)
      router.push('/login')
    }, 200)
    return
  }
  const content = (commentContent.value || '').trim()
  if (!content) {
    ElMessage.error('评论内容不能为空')
    return
  }
  commentApi
    .add(route.params.id, content)
    .then(() => {
      ElMessage.success('评论成功')
      commentContent.value = ''
      commentPage.value = 1
      queryComments()
    })
    .catch((err) => ElMessage.error(err))
}

function deleteComment(c) {
  ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => commentApi.remove(c.id))
    .then(() => {
      ElMessage.success('评论已删除')
      commentPage.value = 1
      queryComments()
    })
    .catch((err) => {
      if (err !== 'cancel') ElMessage.error(err)
    })
}

function addLike() {
  blogApi
    .like(blog.value.id)
    .then(() => {
      blogApi
        .get(blog.value.id)
        .then(({ data }) => {
          data.images = data.images ? data.images.split(',') : []
          blog.value = data
          queryLikeList(blog.value.id)
        })
        .catch((err) => ElMessage.error(err))
    })
    .catch((err) => {
      ElMessage.error(err)
    })
}

function isFollowed() {
  followApi
    .orNot(blog.value.userId)
    .then(({ data }) => (followed.value = data))
    .catch((err) => ElMessage.error(err))
}

function follow() {
  followApi
    .follow(blog.value.userId, !followed.value)
    .then(() => {
      ElMessage.success(followed.value ? '已取消关注' : '已关注')
      followed.value = !followed.value
    })
    .catch((err) => ElMessage.error(err))
}

function formatTime(b) {
  // 无效日期（如 new Date(undefined)）返回空串，避免渲染出 NaN年NaN月NaN日
  if (!b || isNaN(b.getTime())) return ''
  return (
    b.getFullYear() +
    '年' +
    (b.getMonth() + 1) +
    '月' +
    b.getDate() +
    '日 '
  )
}

function queryLoginUser() {
  // 查询当前登录用户信息（需登录：未登录跳过，避免 401 全局跳登录）
  if (!sessionStorage.getItem('token')) return
  userApi
    .me()
    .then(({ data }) => {
      user.value = data
      if (user.value.id !== blog.value.userId) {
        isFollowed()
      }
    })
    .catch((err) => ElMessage.error(err))
}

function deleteBlog() {
  // 仅作者可见的删除入口，二次确认后删除
  ElMessageBox.confirm('删除后不可恢复，确定删除这篇笔记吗？', '删除笔记', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => blogApi.del(blog.value.id))
    .then(() => {
      ElMessage.success('笔记已删除')
      router.push('/profile')
    })
    .catch((err) => {
      if (err !== 'cancel') ElMessage.error(err)
    })
}
</script>

<style>
/* 图片轮播指示器（增强：显示当前第几张） */
.blog-detail-page .indicator-dot {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background-color: rgba(0, 0, 0, 0.5); /* 保留半透明黑底，符合设计规范 */
  color: #fff;
  font-size: 12px;
  z-index: 2;
}
/* 轮播左右切换箭头 */
.blog-detail-page .swiper-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 3;
  transition: background 0.2s ease;
  user-select: none;
}
.blog-detail-page .swiper-arrow:hover {
  background: rgba(0, 0, 0, 0.55);
}
.blog-detail-page .swiper-prev {
  left: 10px;
}
.blog-detail-page .swiper-next {
  right: 10px;
}
/* 头部右侧分享按钮（覆盖全局 .header-share 的 10% 宽度） */
.blog-detail-page .header-share {
  width: auto;
  min-width: 36px;
  text-align: center;
  font-size: 18px;
  color: var(--brand);
  font-weight: bold;
  cursor: pointer;
}
/* 正文容器（底部留白避免遮挡底部导航） */
.blog-detail-page .detail-body {
  padding-bottom: 70px;
}
/* 关联店铺卡片信息列 */
.blog-detail-page .shop-card-info {
  flex: 1;
  min-width: 0;
}
/* 点赞数文案 */
.blog-detail-page .zan-count {
  margin-left: 10px;
  text-align: center;
  line-height: 24px;
}
/* 评论空态 */
.blog-detail-page .comments-empty {
  text-align: center;
  color: var(--text-muted);
  padding: 20px 0;
  font-size: 14px;
}
/* 评论内容/元信息/删除 */
.blog-detail-page .comment-content {
  padding: 5px 0;
  font-size: 14px;
}
.blog-detail-page .comment-meta {
  font-size: 12px;
  color: var(--text-muted);
}
.blog-detail-page .comment-delete {
  color: var(--brand);
  margin-left: 10px;
  cursor: pointer;
}
/* 笔记不存在空态 */
.blog-detail-page .blog-missing {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.blog-detail-page .blog-missing-text {
  font-size: 15px;
  color: var(--text-weak);
}
.blog-detail-page .blog-missing-btn {
  padding: 8px 24px;
  border-radius: var(--radius-pill);
  background: var(--brand);
  color: var(--brand-on);
  box-shadow: var(--shadow-brand);
  font-size: 14px;
  cursor: pointer;
}
/* 加载更多评价 */
.blog-detail-page .load-more {
  display: flex;
  justify-content: center;
  padding: 15px 0;
  border-top: 1px solid #f1f1f1;
  margin-top: 10px;
  color: var(--text-muted);
  cursor: pointer;
}
</style>
