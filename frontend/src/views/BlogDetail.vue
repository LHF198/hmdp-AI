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
      <div
        class="blog-info-box"
        ref="swiper"
        :style="swiperHeight ? { height: swiperHeight + 'px' } : {}"
        @touchstart="moveStart"
        @touchmove="moving"
        @touchend="moveEnd"
        @mousedown="mouseDown"
        @mousemove="mouseMoving"
        @mouseup="mouseEnd"
        @mouseleave="mouseEnd"
      >
        <div class="swiper-item" v-for="(img, i) in blog.images" :key="i">
          <img :src="img" alt="" draggable="false" @load="onImgLoad(i, $event)" />
        </div>
        <div class="indicator-dot" v-if="blog.images && blog.images.length > 1">
          {{ active + 1 }}/{{ blog.images.length }}
        </div>
        <!-- 左右切换箭头（多张图时显示） -->
        <div
          v-if="blog.images && blog.images.length > 1"
          class="swiper-arrow swiper-prev"
          @click.stop="prevSlide"
        >
          <el-icon :size="20"><ArrowLeft /></el-icon>
        </div>
        <div
          v-if="blog.images && blog.images.length > 1"
          class="swiper-arrow swiper-next"
          @click.stop="nextSlide"
        >
          <el-icon :size="20"><ArrowRight /></el-icon>
        </div>
      </div>

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
      <div class="shop-basic" @click="toShopDetail" title="点击查看商铺详情" v-if="shop.id">
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

      <!-- 点赞区 -->
      <div class="zan-box">
        <div>
          <svg
            t="1646634642977"
            class="icon"
            viewBox="0 0 1024 1024"
            version="1.1"
            xmlns="http://www.w3.org/2000/svg"
            width="20"
            height="20"
          >
            <path
              d="M160 944c0 8.8-7.2 16-16 16h-32c-26.5 0-48-21.5-48-48V528c0-26.5 21.5-48 48-48h32c8.8 0 16 7.2 16 16v448zM96 416c-53 0-96 43-96 96v416c0 53 43 96 96 96h96c17.7 0 32-14.3 32-32V448c0-17.7-14.3-32-32-32H96zM505.6 64c16.2 0 26.4 8.7 31 13.9 4.6 5.2 12.1 16.3 10.3 32.4l-23.5 203.4c-4.9 42.2 8.6 84.6 36.8 116.4 28.3 31.7 68.9 49.9 111.4 49.9h271.2c6.6 0 10.8 3.3 13.2 6.1s5 7.5 4 14l-48 303.4c-6.9 43.6-29.1 83.4-62.7 112C815.8 944.2 773 960 728.9 960h-317c-33.1 0-59.9-26.8-59.9-59.9v-455c0-6.1 1.7-12 5-17.1 69.5-109 106.4-234.2 107-364h41.6z m0-64h-44.9C427.2 0 400 27.2 400 60.7c0 127.1-39.1 251.2-112 355.3v484.1c0 68.4 55.5 123.9 123.9 123.9h317c122.7 0 227.2-89.3 246.3-210.5l47.9-303.4c7.8-49.4-30.4-94.1-80.4-94.1H671.6c-50.9 0-90.5-44.4-84.6-95l23.5-203.4C617.7 55 568.7 0 505.6 0z"
              :fill="blog.isLike ? BRAND_COLOR : TEXT_SECONDARY"
            ></path>
          </svg>
        </div>
        <div class="zan-list">
          <div class="user-icon-mini" v-for="u in likes" :key="u.id">
            <img :src="u.icon || defaultIcon" alt="" />
          </div>
          <div class="zan-count">
            {{ blog.liked }}人点赞
          </div>
        </div>
      </div>

      <div class="blog-divider"></div>

      <!-- 评论区 -->
      <div class="blog-comments">
        <div class="comments-head">
          <div>网友评价 <span>（{{ commentTotal }}）</span></div>
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
                  v-if="user && user.id === c.user_id"
                  class="comment-delete"
                  @click="deleteComment(c)"
                >删除</span>
              </div>
            </div>
          </div>
          <div
            v-if="comments.length < commentTotal"
            class="load-more"
            @click="loadMoreComments"
          >
            加载更多评价
          </div>
        </div>
      </div>
      <div class="blog-divider"></div>
    </div>

    <!-- 底部栏：点赞 + 评论输入（Teleport 到 body：#app 的 backdrop-filter 会成为
         fixed 元素的包含块，导致底栏随内容滚动而非贴底） -->
    <Teleport to="body" v-if="!loadError">
      <div class="foot blog-detail-foot">
        <!-- 点赞区 -->
        <div class="foot-like" @click="addLike">
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
              :fill="blog.isLike ? BRAND_COLOR : '#999'"
            ></path>
          </svg>
          <span class="like-count" :class="{ liked: blog.isLike }">{{ blog.liked }}</span>
        </div>

        <!-- 评论输入区 -->
        <div class="comment-bar">
          <input
            class="comment-input"
            v-model="commentContent"
            placeholder="说点什么，温柔一点～"
            @keyup.enter="sendComment"
          />
          <button class="send-btn" :class="{ active: commentContent.trim() }" @click="sendComment">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
            </svg>
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
// 笔记详情页：展示笔记图文/作者/关联店铺卡片，支持点赞、评论、关注与分享；
// 路由参数 blogId 驱动数据加载（旧 MPA 页面 blog-detail.html 的 SPA 迁移版）
import { ref, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { blogApi, commentApi } from '@/api/blog'
import { shopApi } from '@/api/shop'
import { userApi } from '@/api/user'
import { followApi } from '@/api/follow'
import { BRAND_COLOR, TEXT_SECONDARY } from '@/utils/colors'
import { useUserStore } from '@/stores/user'
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

// 触摸轮播状态
const swiper = ref(null)
const _width = ref(0)
const items = ref([])
const active = ref(0)
const imgRatios = ref({}) // 每张图的自然宽高比（width / height）
const swiperHeight = ref(0) // 轮播容器高度：随当前图比例自适应，保证图片尽量完整展示
const duration = 300
const sensitivity = 60
const resistance = 0.3
const start = { x: 0, y: 0 }
const move = { x: 0, y: 0 }
let isMoving = false
let isMouseDragging = false
let mouseMoved = false // 鼠标是否真实拖动过（区分点击与拖拽）
let lastTouchTime = 0 // 触屏点击会合成鼠标事件，用于去重

onMounted(() => {
  queryBlogById(route.params.id)
  queryComments()
})

// 图片加载完成后初始化轮播（宽度由 CSS 决定，无需等图片加载）
watch(
  () => blog.value.images,
  (imgs) => {
    if (imgs && imgs.length) {
      nextTick(() => {
        initSwiper()
        // 延迟再次初始化，确保 DOM 渲染完成（解决首次 _width 为 0 的问题）
        setTimeout(() => initSwiper(), 100)
      })
    }
  }
)

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

function formatCommentTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return (
    d.getMonth() + 1 + '月' + d.getDate() + '日 ' +
    d.getHours() + ':' + (d.getMinutes() < 10 ? '0' + d.getMinutes() : d.getMinutes())
  )
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
  // 查询当前登录用户信息
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

// ===== 图片触摸轮播（迁移自旧 blog-detail.html 的手写 swiper） =====
// 图片加载完成后记录自然比例；若是当前展示图则同步调整容器高度
function onImgLoad(i, e) {
  const img = e.target
  if (img.naturalWidth && img.naturalHeight) {
    imgRatios.value[i] = img.naturalWidth / img.naturalHeight
    if (i === active.value) {
      applySwiperHeight()
    }
  }
}

// 按当前图比例自适应轮播高度：最小 200px，最大 75vh（防极长图撑爆页面）
function applySwiperHeight() {
  const ratio = imgRatios.value[active.value]
  if (!ratio || !swiper.value) return
  const w = swiper.value.offsetWidth
  if (!w) return
  const h = w / ratio
  swiperHeight.value = Math.round(Math.min(Math.max(h, 200), window.innerHeight * 0.75))
}

function initSwiper() {
  if (!swiper.value) return
  items.value = swiper.value.querySelectorAll('.swiper-item')
  const w = swiper.value.offsetWidth || document.documentElement.offsetWidth
  if (w > 0) {
    _width.value = w
  }
  // 如果 _width 仍为 0，无法正确布局，直接返回等下次重试
  if (_width.value <= 0) return
  setTransform()
  setTransition('none')
}

function setTransform(offset) {
  offset = offset || 0
  items.value.forEach((item, i) => {
    const distance = (i - active.value) * _width.value + offset
    const transform = `translate3d(${distance}px, 0, 0)`
    item.style.webkitTransform = transform
    item.style.transform = transform
  })
}

function setTransition(d) {
  d = d || duration
  d = typeof d === 'number' ? d + 'ms' : d
  items.value.forEach((item) => {
    item.style.webkitTransition = d
    item.style.transition = d
  })
}

function moveStart(e) {
  // 点在箭头按钮上时不进入拖拽逻辑，交给箭头的 click 处理
  if (e.target.closest && e.target.closest('.swiper-arrow')) return
  lastTouchTime = Date.now()
  start.x = e.changedTouches[0].pageX
  start.y = e.changedTouches[0].pageY
  setTransition('none')
}

function moving(e) {
  e.preventDefault()
  e.stopPropagation()
  const distanceX = e.changedTouches[0].pageX - start.x
  const distanceY = e.changedTouches[0].pageY - start.y
  if (Math.abs(distanceX) > Math.abs(distanceY)) {
    isMoving = true
    move.x = start.x + distanceX
    move.y = start.y + distanceY
    // 首项右滑/末项左滑加阻力，形成拉弹簧效果
    let dx = distanceX
    if (
      (active.value === 0 && distanceX > 0) ||
      (active.value === items.value.length - 1 && distanceX < 0)
    ) {
      dx = distanceX * resistance
    }
    setTransform(dx)
  }
}

function moveEnd(e) {
  if (isMoving) {
    e.preventDefault()
    e.stopPropagation()
    const distance = move.x - start.x
    if (Math.abs(distance) > sensitivity) {
      if (distance < 0) {
        nextSlide()
      } else {
        prevSlide()
      }
    } else {
      back()
    }
    reset()
    isMoving = false
  }
}

// 鼠标拖拽支持（桌面端）
function mouseDown(e) {
  if (e.button !== 0) return // 仅左键
  // 点在箭头按钮上时不启动拖拽，避免 mouseup 误判为滑动导致跳页
  if (e.target.closest && e.target.closest('.swiper-arrow')) return
  // 触摸操作后的合成鼠标事件跳过，防止一次手势触发两次切换
  if (Date.now() - lastTouchTime < 500) return
  isMouseDragging = true
  mouseMoved = false
  start.x = e.pageX
  start.y = e.pageY
  setTransition('none')
}

function mouseMoving(e) {
  if (!isMouseDragging) return
  const distanceX = e.pageX - start.x
  const distanceY = e.pageY - start.y
  if (Math.abs(distanceX) > Math.abs(distanceY)) {
    e.preventDefault()
    mouseMoved = true
    move.x = e.pageX
    move.y = e.pageY
    let dx = distanceX
    if (
      (active.value === 0 && distanceX > 0) ||
      (active.value === items.value.length - 1 && distanceX < 0)
    ) {
      dx = distanceX * resistance
    }
    setTransform(dx)
  }
}

function mouseEnd(e) {
  if (!isMouseDragging) return
  isMouseDragging = false
  // 未发生真实位移（纯点击/误触）只做回弹，避免把点击误判为滑动
  if (!mouseMoved) {
    back()
    return
  }
  const distance = move.x - start.x
  if (Math.abs(distance) > sensitivity) {
    if (distance < 0) {
      nextSlide()
    } else {
      prevSlide()
    }
  } else {
    back()
  }
  reset()
}

function nextSlide() {
  go(active.value + 1)
}

function prevSlide() {
  go(active.value - 1)
}

function reset() {
  start.x = 0
  start.y = 0
  move.x = 0
  move.y = 0
}

function back() {
  setTransition()
  setTransform()
}

function go(index) {
  active.value = index
  if (active.value < 0) {
    active.value = 0
  } else if (active.value > items.value.length - 1) {
    active.value = items.value.length - 1
  }
  setTransition()
  setTransform()
  // 切换后按新图比例调整容器高度（该图未加载完成时维持原高度，load 后再修正）
  applySwiperHeight()
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
