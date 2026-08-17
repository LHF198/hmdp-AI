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
          class="logout-btn logout-btn--brand"
          @click="sign"
          v-if="user && user.id"
        >
          {{ signedToday ? '已签到(' + signCount + '天)' : '签到' }}
        </div>
        <div class="logout-btn" @click="pwdDialogVisible = true" title="设置/修改登录密码">修改密码</div>
        <div class="logout-btn" @click="logout">退出登录</div>
      </div>
      <div class="introduce" @click="toEdit" title="点击编辑简介">
        <span v-if="info.introduce">{{ info.introduce }}</span>
        <span v-else>添加个人简介，让大家更好的认识你 <el-icon :size="14"><Edit /></el-icon></span>
      </div>
    </div>

    <!-- Tab 内容区 -->
    <div class="content">
      <el-tabs v-model="activeName" @tab-click="handleClick">
        <el-tab-pane label="笔记" name="1">
          <EmptyState v-if="blogs.length === 0" text="还没有发布过笔记，去首页写一篇吧" />
          <div v-for="b in blogs" :key="b.id" class="blog-item">
            <div class="blog-img">
              <img :src="firstImage(b.images)" alt="" />
            </div>
            <div class="blog-info">
              <div class="blog-title">{{ b.title }}</div>
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
          <EmptyState v-if="myComments.length === 0" text="还没有发过评价，去笔记详情页评一条吧" />
          <div
            v-for="c in myComments"
            :key="c.id"
            class="glass-card glass-card--tappable comment-row"
            @click="toBlogById(c.blog_id)"
          >
            <div class="comment-row-text">{{ c.content }}</div>
            <div class="comment-row-meta">
              评《{{ c.blog_title || '笔记' }}》 · {{ relativeTime(c.create_time) }}
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`粉丝(${info.fans || 0})`" name="3">
          <EmptyState v-if="fans.length === 0" text="还没有粉丝，多发笔记吸引关注吧" />
          <div v-for="f in fans" :key="f.id" class="glass-card user-row fan-row">
            <img class="user-row-avatar" :src="f.user_icon || defaultIcon" alt="" />
            <div>
              <div class="user-row-name">{{ f.user_nick_name }}</div>
              <div class="user-row-meta">{{ relativeTime(f.create_time) }} 关注了你</div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`关注(${info.followee || 0})`" name="4">
          <EmptyState v-if="blogs2.length === 0" text="还没有关注的人，去首页看看热门笔记吧" />
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
                  <LikeIcon :active="b.isLike" />
                  {{ b.liked }}
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <FootBar :active-btn="4" />

    <!-- 设置/修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="设置 / 修改密码" width="320px">
      <div class="pwd-field">
        <label class="pwd-label">原密码</label>
        <el-input
          v-model="pwdForm.oldPassword"
          type="password"
          show-password
          placeholder="未设置过密码可留空"
        />
      </div>
      <div class="pwd-field">
        <label class="pwd-label">新密码</label>
        <el-input
          v-model="pwdForm.newPassword"
          type="password"
          show-password
          placeholder="4~32位字母、数字或下划线"
        />
      </div>
      <div class="pwd-field">
        <label class="pwd-label">确认新密码</label>
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </div>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
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
import { useUserStore } from '@/stores/user'
import { messageApi } from '@/api/message'
import { relativeTime } from '@/utils/date'
import EmptyState from '@/components/EmptyState.vue'
import LikeIcon from '@/components/LikeIcon.vue'
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
const pwdDialogVisible = ref(false) // 修改密码弹窗开关
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' }) // 修改密码表单

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
      // 统一走 store 清理（token + 登录态），避免 store 与 sessionStorage 不同步
      useUserStore().logout()
      router.push('/')
    })
    .catch((err) => ElMessage.error(err))
}

function submitPassword() {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  // 前端先做格式校验，避免无效请求
  if (!newPassword || !/^\w{4,32}$/.test(newPassword)) {
    ElMessage.error('新密码格式不正确（4~32位字母、数字或下划线）')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  userApi
    .setPassword({ oldPassword: oldPassword || '', newPassword })
    .then(() => {
      ElMessage.success('密码设置成功，下次登录可使用密码登录')
      pwdDialogVisible.value = false
      pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
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

<style scoped>
/* 本页特有的行样式；通用玻璃卡片/空态/用户行样式见 styles/components.css，
   .info-page 布局与关注 Tab 的滚动行为见 styles/page-info.css */
.comment-row,
.fan-row {
  margin-bottom: 10px;
}
.comment-row-text {
  font-size: 14px;
  color: var(--text-strong);
}
.comment-row-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
</style>
