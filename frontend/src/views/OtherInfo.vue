<template>
  <div>
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">&nbsp;&nbsp;&nbsp;</div>
    </div>
    <div class="basic">
      <div class="basic-icon">
        <img :src="user.icon || defaultIcon" alt="" />
      </div>
      <div class="basic-info">
        <div class="name">{{ user.nickName }}</div>
        <span>杭州</span>
      </div>
      <div class="logout-btn" @click="follow" style="text-align: center">
        {{ followed ? '取消关注' : '关注' }}
      </div>
    </div>
    <div class="introduce">
      <span v-if="info.introduce">{{ info.introduce }}</span>
      <span v-else>这个人很懒，什么都没有留下</span>
    </div>
    <div class="content">
      <el-tabs v-model="activeName" @tab-click="handleClick">
        <el-tab-pane label="笔记" name="1">
          <div v-for="b in blogs" :key="b.id" class="blog-item">
            <div class="blog-img">
              <img :src="b.images.split(',')[0]" alt="" />
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
          </div>
        </el-tab-pane>
        <el-tab-pane label="共同关注" name="2">
          <div>你们都关注了：</div>
          <div class="follow-info" v-for="u in commonFollows" :key="u.id">
            <div class="follow-info-icon" @click="toOtherInfo(u.id)">
              <img :src="u.icon || defaultIcon" alt="" />
            </div>
            <div class="follow-info-name">
              <div class="name">{{ u.nickName }}</div>
            </div>
            <div class="follow-info-btn" @click="toOtherInfo(u.id)">去主页看看</div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <FootBar :active-btn="0" />
  </div>
</template>

<script setup>
// 他人主页：展示他人头像昵称/简介/笔记列表，支持关注/取关与共同关注展示；路由参数 id
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { blogApi } from '@/api/blog'
import { followApi } from '@/api/follow'
import defaultIcon from '../../html/hmdp/imgs/icons/default-icon.png'
import thumbup from '../../html/hmdp/imgs/thumbup.png'

const route = useRoute()
const router = useRouter()

const user = ref({})
const activeName = ref('1')
const info = ref({})
const blogs = ref([])
const followed = ref(false) // 是否关注了
const commonFollows = ref([]) // 共同关注

onMounted(() => {
  queryUser()
  queryLoginUser()
})

function queryBlogs() {
  blogApi
    .ofUser({ id: user.value.id, current: 1 })
    .then(({ data }) => (blogs.value = data))
    .catch((err) => ElMessage.error(err))
}

function queryLoginUser() {
  // 查询当前登录用户信息
  userApi
    .me()
    .then(({ data }) => {})
    .catch(console.log)
}

function queryUser() {
  // 查询目标用户信息
  const id = route.params.id
  userApi
    .getUser(id)
    .then(({ data }) => {
      user.value = data
      // 查询用户详情
      queryUserInfo()
      // 查询用户笔记
      queryBlogs()
      // 是否被关注
      isFollowed()
    })
    .catch(console.log)
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
      // 保存用户详情
      info.value = data
    })
    .catch((err) => {
      ElMessage.error(err)
    })
}

function isFollowed() {
  followApi
    .orNot(user.value.id)
    .then(({ data }) => (followed.value = data))
    .catch((err) => ElMessage.error(err))
}

function queryCommonFollow() {
  followApi
    .common(user.value.id)
    .then(({ data }) => (commonFollows.value = data))
    .catch((err) => {
      ElMessage.error(err)
    })
}

function follow() {
  followApi
    .follow(user.value.id, !followed.value)
    .then(() => {
      ElMessage.success(followed.value ? '已取消关注' : '已关注')
      followed.value = !followed.value
    })
    .catch((err) => ElMessage.error(err))
}

function handleClick(t) {
  // Element Plus 2.9 的 tab-click 回调参数是 ElTabPane 组件实例（t.name 为组件名），
  // 需从 paneName/props 取 tab 的 name（旧 Element UI 的 t.name 语义已变更）
  const name = typeof t === 'string' ? t : t.paneName?.value ?? t.props?.name
  if (name === '2') {
    queryCommonFollow()
  }
}

function toOtherInfo(id) {
  router.push('/user/' + id)
}
</script>

<style>
/* 共同关注列表样式（旧 other-info.html 中类样式缺失，按玻璃拟态风格补齐） */
.follow-info {
  display: flex;
  align-items: center;
  padding: 12px 4px;
  border-bottom: 1px solid rgba(31, 45, 61, 0.08);
}
.follow-info-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  cursor: pointer;
}
.follow-info-icon img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #eee;
}
.follow-info-name {
  flex: 1;
  padding: 0 12px;
}
.follow-info-name .name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-strong);
}
.follow-info-btn {
  padding: 5px 14px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: var(--text-strong);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
</style>
