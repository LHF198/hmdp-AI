<template>
  <div class="info-page">
    <!-- 页面头部 -->
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">个人主页&nbsp;&nbsp;&nbsp;</div>
    </div>

    <!-- 用户资料卡 -->
    <div class="basic">
      <div class="basic-top-row">
        <div class="basic-icon">
          <img :src="user.icon || defaultIcon" alt="" />
        </div>
        <div class="basic-info">
          <div class="name">{{ user.nickName }}</div>
          <span>{{ info.city || '杭州' }}</span>
          <div class="edit-btn" @click="toEdit">编辑资料</div>
        </div>
        <div
          class="logout-btn"
          style="margin-right: 8px; color: #ff6633; border-color: #ff6633"
          @click="sign"
          v-if="user && user.id"
        >
          {{ signedToday ? '已签到(' + signCount + '天)' : '签到' }}
        </div>
        <div class="logout-btn" @click="logout">退出登录</div>
      </div>
      <div class="introduce" @click="toEdit" title="点击编辑简介" style="cursor: pointer">
        <span v-if="info.introduce">{{ info.introduce }}</span>
        <span v-else>添加个人简介，让大家更好的认识你 <el-icon :size="14"><Edit /></el-icon></span>
      </div>
    </div>

    <!-- Tab 内容区 -->
    <div class="content">
      <el-tabs v-model="activeName" @tab-click="handleClick">
        <el-tab-pane label="笔记" name="1">
          <div v-if="blogs.length === 0" style="text-align: center; color: #82848a; padding: 40px 0; font-size: 14px">
            还没有发布过笔记，去首页写一篇吧
          </div>
          <div v-for="b in blogs" :key="b.id" class="blog-item">
            <div class="blog-img">
              <img :src="firstImage(b.images)" alt="" />
            </div>
            <div class="blog-info">
              <div class="blog-title" v-html="b.title"></div>
              <div class="blog-liked">
                <img :src="thumbup" alt="" /> {{ b.liked }}
              </div>
              <div class="blog-comments">
                <el-icon :size="14"><ChatDotRound /></el-icon> {{ b.comments }}
              </div>
            </div>
            <div class="blog-del" @click.stop="deleteBlog(b)" title="删除笔记">
              <el-icon :size="18"><Delete /></el-icon>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="评价" name="2">
          <div v-if="myComments.length === 0" style="text-align: center; color: #82848a; padding: 40px 0; font-size: 14px">
            还没有发过评价，去笔记详情页评一条吧
          </div>
          <div
            v-for="c in myComments"
            :key="c.id"
            style="background: rgba(255,255,255,0.85); border-radius: 10px; padding: 10px 12px; margin-bottom: 10px; cursor: pointer"
            @click="toBlogById(c.blog_id)"
          >
            <div style="font-size: 14px; color: #222">{{ c.content }}</div>
            <div style="font-size: 12px; color: #999; margin-top: 4px">
              评《{{ c.blog_title || '笔记' }}》 · {{ relativeTime(c.create_time) }}
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`粉丝(${info.fans || 0})`" name="3">
          <div v-if="fans.length === 0" style="text-align: center; color: #82848a; padding: 40px 0; font-size: 14px">
            还没有粉丝，多发笔记吸引关注吧
          </div>
          <div
            v-for="f in fans"
            :key="f.id"
            style="display: flex; align-items: center; background: rgba(255,255,255,0.85); border-radius: 10px; padding: 10px 12px; margin-bottom: 10px"
          >
            <img
              :src="f.user_icon || defaultIcon"
              alt=""
              style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover; margin-right: 10px"
            />
            <div>
              <div style="font-size: 14px; color: #222">{{ f.user_nick_name }}</div>
              <div style="font-size: 12px; color: #999; margin-top: 2px">{{ relativeTime(f.create_time) }} 关注了你</div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`关注(${info.followee || 0})`" name="4">
          <div v-if="blogs2.length === 0" style="text-align: center; color: #82848a; padding: 40px 0; font-size: 14px">
            还没有关注的人，去首页看看热门笔记吧
          </div>
          <div class="blog-list" @scroll="onScroll">
            <div class="blog-box" v-for="b in blogs2" :key="b.id">
              <div class="blog-img2" @click="toBlogDetail(b)">
                <img :src="b.img" alt="" />
              </div>
              <div class="blog-title">{{ b.title }}</div>
              <div class="blog-foot">
                <div class="blog-user-icon">
                  <img :src="b.icon || defaultIcon" alt="" />
                </div>
                <div class="blog-user-name">{{ b.name }}</div>
                <div class="blog-liked" @click="addLike(b)">
                  <svg
                    t="1646634642977"
                    class="icon"
                    viewBox="0 0 1024 1024"
                    version="1.1"
                    xmlns="http://www.w3.org/2000/svg"
                    width="14"
                    height="14"
                  >
                    <path
                      d="M160 944c0 8.8-7.2 16-16 16h-32c-26.5 0-48-21.5-48-48V528c0-26.5 21.5-48 48-48h32c8.8 0 16 7.2 16 16v448zM96 416c-53 0-96 43-96 96v416c0 53 43 96 96 96h96c17.7 0 32-14.3 32-32V448c0-17.7-14.3-32-32-32H96zM505.6 64c16.2 0 26.4 8.7 31 13.9 4.6 5.2 12.1 16.3 10.3 32.4l-23.5 203.4c-4.9 42.2 8.6 84.6 36.8 116.4 28.3 31.7 68.9 49.9 111.4 49.9h271.2c6.6 0 10.8 3.3 13.2 6.1s5 7.5 4 14l-48 303.4c-6.9 43.6-29.1 83.4-62.7 112C815.8 944.2 773 960 728.9 960h-317c-33.1 0-59.9-26.8-59.9-59.9v-455c0-6.1 1.7-12 5-17.1 69.5-109 106.4-234.2 107-364h41.6z m0-64h-44.9C427.2 0 400 27.2 400 60.7c0 127.1-39.1 251.2-112 355.3v484.1c0 68.4 55.5 123.9 123.9 123.9h317c122.7 0 227.2-89.3 246.3-210.5l47.9-303.4c7.8-49.4-30.4-94.1-80.4-94.1H671.6c-50.9 0-90.5-44.4-84.6-95l23.5-203.4C617.7 55 568.7 0 505.6 0z"
                      :fill="b.isLike ? '#ff6633' : '#82848a'"
                    ></path>
                  </svg>
                  {{ b.liked }}
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <FootBar :active-btn="4" />
  </div>
</template>

<script setup>
// 个人中心：用户信息与统计（粉丝/关注）、我的笔记/关注 Feed/粉丝/我的评价 Tab、
// 每日签到（Redis BitMap）、编辑资料与退出登录入口
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api/user'
import { blogApi, commentApi } from '@/api/blog'
import { messageApi } from '@/api/message'
import { relativeTime } from '@/utils/date'
import defaultIcon from '../../html/hmdp/imgs/icons/default-icon.png'
import thumbup from '../../html/hmdp/imgs/thumbup.png'

const router = useRouter()

const user = ref({})
const activeName = ref('1')
const info = ref({ fans: 0, followee: 0 })
const blogs = ref([]) // 我的笔记
const blogs2 = ref([]) // 关注的人的笔记
const fans = ref([]) // 关注我的用户列表
const myComments = ref([]) // 我发出的评价
const signCount = ref(0) // 本月连续签到天数
const signedToday = ref(false) // 今日是否已签到
const params = ref({ minTime: 0, offset: 0 }) // 关注 feed 游标
const isReachBottom = ref(false)

onMounted(() => {
  queryUser()
})

function queryUser() {
  // 查询当前登录用户信息，未登录跳转登录页
  userApi
    .me()
    .then(({ data }) => {
      user.value = data
      queryUserInfo()
      queryBlogs()
      querySignCount()
    })
    .catch((err) => {
      ElMessage.error(err)
      setTimeout(() => router.replace('/login'), 1000)
    })
}

function goBack() {
  router.back()
}

function queryUserInfo() {
  userApi
    .getUserInfo(user.value.id)
    .then(({ data }) => {
      if (!data) {
        return
      }
      info.value = data
      // 保存到本地（与旧行为一致，编辑资料页复用）
      sessionStorage.setItem('userInfo', JSON.stringify(data))
    })
    .catch((err) => {
      ElMessage.error(err)
    })
}

function queryBlogs() {
  blogApi
    .ofMe(1)
    .then(({ data }) => (blogs.value = data))
    .catch((err) => ElMessage.error(err))
}

function deleteBlog(b) {
  // 删除自己的笔记，成功后从列表移除
  ElMessageBox.confirm('删除后不可恢复，确定删除笔记《' + (b.title || '') + '》吗？', '删除笔记', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => blogApi.del(b.id))
    .then(() => {
      ElMessage.success('笔记已删除')
      blogs.value = blogs.value.filter((x) => x.id !== b.id)
    })
    .catch((err) => {
      if (err !== 'cancel') ElMessage.error(err)
    })
}

function queryBlogsOfFollow(clear) {
  if (clear) {
    params.value.offset = 0
    params.value.minTime = new Date().getTime() + 1
  }
  const { minTime, offset } = params.value
  blogApi
    .ofFollow({ offset, lastId: minTime || new Date().getTime() + 1 })
    .then(({ data }) => {
      if (!data) {
        return
      }
      const { list, ...rest } = data
      list.forEach((b) => (b.img = (b.images || '').split(',')[0]))
      blogs2.value = clear ? list : blogs2.value.concat(list)
      params.value = rest
    })
    .catch((e) => console.log(e))
}

function signFlagKey() {
  // 今日签到状态的本地缓存 key，按用户+年月日区分
  const d = new Date()
  return 'signed_' + user.value.id + '_' + d.getFullYear() + (d.getMonth() + 1) + '_' + d.getDate()
}

function sign() {
  if (signedToday.value) {
    ElMessage.info('今日已签到')
    return
  }
  userApi
    .sign()
    .then(() => {
      localStorage.setItem(signFlagKey(), '1')
      querySignCount(true)
    })
    .catch((err) => ElMessage.error(err))
}

function querySignCount(justSigned) {
  userApi
    .signCount()
    .then(({ data }) => {
      signCount.value = data || 0
      signedToday.value = localStorage.getItem(signFlagKey()) === '1'
      if (justSigned) {
        ElMessage.success('签到成功，本月已连续签到' + signCount.value + '天')
      }
    })
    .catch((e) => console.log(e))
}

function logout() {
  userApi
    .logout()
    .then(() => {
      // 清理 session 并回首页
      sessionStorage.removeItem('token')
      router.push('/')
    })
    .catch((err) => ElMessage.error(err))
}

function handleClick(t) {
  // Element Plus 2.9 的 tab-click 回调参数是 ElTabPane 组件实例（t.name 为组件名），
  // 需从 paneName/props 取 tab 的 name（旧 Element UI 的 t.name 语义已变更）
  const name = typeof t === 'string' ? t : t.paneName?.value ?? t.props?.name
  if (name === '4') {
    queryBlogsOfFollow(true)
  } else if (name === '2') {
    queryMyComments()
  } else if (name === '3') {
    queryFans()
  }
}

function queryFans() {
  // 查询关注我的用户（复用消息中心的关注通知接口）
  messageApi
    .follows()
    .then(({ data }) => (fans.value = data || []))
    .catch((err) => ElMessage.error(err))
}

function queryMyComments() {
  // 查询我发出的评价（含关联笔记标题）
  commentApi
    .ofMe()
    .then(({ data }) => (myComments.value = data || []))
    .catch((err) => ElMessage.error(err))
}

function toEdit() {
  router.push('/profile/edit')
}

function toBlogById(blogId) {
  router.push('/blog/' + blogId)
}

function toBlogDetail(b) {
  router.push('/blog/' + b.id)
}

function addLike(b) {
  // 乐观更新：立即翻转状态，失败时回退
  b.isLike = !b.isLike
  b.liked += b.isLike ? 1 : -1
  blogApi
    .like(b.id)
    .then(() => queryBlogById(b))
    .catch((err) => {
      b.isLike = !b.isLike
      b.liked += b.isLike ? 1 : -1
      ElMessage.error(err)
    })
}

function queryBlogById(b) {
  blogApi
    .get(b.id)
    .then(({ data }) => {
      b.liked = data.liked
      b.isLike = data.isLike
    })
    .catch(() => {})
}

function onScroll(e) {
  const scrollTop = e.target.scrollTop
  const offsetHeight = e.target.offsetHeight
  const scrollHeight = e.target.scrollHeight
  if (scrollTop === 0) {
    // 到顶部了，刷新一次
    queryBlogsOfFollow(true)
  } else if (scrollTop + offsetHeight + 1 > scrollHeight && !isReachBottom.value) {
    isReachBottom.value = true
    // 滚动到底部，加载下一页
    queryBlogsOfFollow()
  } else {
    isReachBottom.value = false
  }
}

function firstImage(images) {
  return (images || '').split(',')[0]
}
</script>

<style>
/* Profile 页的 .blog-list（关注 Tab）不走内部滚动，随页面自然滚动 */
.info-page .blog-list {
  height: auto !important;
  max-height: none !important;
  overflow: visible !important;
  padding-bottom: 80px;
}
</style>
