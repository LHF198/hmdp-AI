<template>
  <div>
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">资料编辑&nbsp;&nbsp;&nbsp;</div>
    </div>
    <div class="edit-container">
      <div class="info-box">
        <div class="info-item">
          <div class="info-label">头像</div>
          <div class="info-btn" style="cursor: pointer" @click="chooseIcon" title="点击更换头像">
            <img width="35" style="border-radius: 50%" :src="user.icon || defaultIcon" alt="" />
            <el-icon :size="16"><ArrowRight /></el-icon>
          </div>
        </div>
        <div class="divider"></div>
        <div class="info-item">
          <div class="info-label">昵称</div>
          <div class="info-btn">
            <el-input
              size="small"
              v-model="user.nickName"
              maxlength="20"
              style="width: 150px; text-align: right"
            ></el-input>
          </div>
        </div>
        <div class="divider"></div>
        <div class="info-item">
          <div class="info-label">个人介绍</div>
          <div class="info-btn">
            <el-input
              size="small"
              v-model="info.introduce"
              maxlength="128"
              placeholder="介绍一下自己"
              style="width: 150px; text-align: right"
            ></el-input>
          </div>
        </div>
      </div>
      <div class="info-box">
        <div class="info-item">
          <div class="info-label">性别</div>
          <div class="info-btn">
            <el-select size="small" v-model="info.gender" placeholder="选择">
              <el-option :value="false" label="男"></el-option>
              <el-option :value="true" label="女"></el-option>
            </el-select>
          </div>
        </div>
        <div class="divider"></div>
        <div class="info-item">
          <div class="info-label">城市</div>
          <div class="info-btn">
            <el-select
              size="small"
              v-model="info.city"
              filterable
              allow-create
              default-first-option
              placeholder="选择"
            >
              <el-option v-for="c in cities" :key="c" :value="c" :label="c"></el-option>
            </el-select>
          </div>
        </div>
        <div class="divider"></div>
        <div class="info-item">
          <div class="info-label">生日</div>
          <div class="info-btn">
            <el-date-picker
              size="small"
              v-model="info.birthday"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="添加"
            ></el-date-picker>
          </div>
        </div>
      </div>
      <div class="info-box" style="text-align: center">
        <el-button
          type="primary"
          round
          style="width: 80%; background: #ff6633; border-color: #ff6633"
          @click="save"
          >保存修改</el-button
        >
      </div>

      <div class="info-box">
        <div class="info-item">
          <div class="info-label">我的积分</div>
          <div class="info-btn">
            <div>查看积分</div>
            <el-icon :size="16"><ArrowRight /></el-icon>
          </div>
        </div>
        <div class="divider"></div>
        <div class="info-item">
          <div class="info-label">会员等级</div>
          <div class="info-btn">
            <div><a href="javascript:void(0)">成为VIP尊享特权</a></div>
            <el-icon :size="16"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>
    <FootBar :active-btn="4" />
  </div>
</template>

<script setup>
// 编辑资料页：修改头像/昵称/性别/生日/所在城市/签名，保存后返回个人中心
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { blogApi } from '@/api/blog'
import defaultIcon from '../../html/hmdp/imgs/icons/default-icon.png'

const router = useRouter()

const user = ref({})
const info = ref({})
const iconChanged = ref(false) // 是否更换了头像
const cities = ['杭州', '北京', '上海', '广州', '深圳', '成都', '南京', '武汉', '西安', '重庆']

onMounted(() => {
  checkLogin()
})

function checkLogin() {
  // 查询用户信息
  userApi
    .me()
    .then(({ data }) => {
      user.value = data
      // 实时回显个人资料（不依赖 sessionStorage，保证直接进入本页也能显示）
      userApi
        .getUserInfo(user.value.id)
        .then(({ data: infoData }) => {
          if (infoData) {
            info.value = infoData
          }
        })
        .catch(() => {})
    })
    .catch((err) => {
      ElMessage.error(err)
      setTimeout(() => router.replace('/login'), 1000)
    })
}

function chooseIcon() {
  // 动态创建文件选择框，上传头像
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = () => {
    if (!input.files || !input.files[0]) {
      return
    }
    blogApi
      .upload(input.files[0])
      .then(({ data }) => {
        // data 是图片相对路径（/blogs/...），补 /imgs 前缀后直接展示，保存时一并提交
        user.value.icon = '/imgs' + data
        iconChanged.value = true
      })
      .catch((err) => ElMessage.error(err))
  }
  input.click()
}

function goBack() {
  router.back()
}

function save() {
  if (!user.value.nickName || !user.value.nickName.trim()) {
    ElMessage.error('昵称不能为空')
    return
  }
  const data = {
    nickName: user.value.nickName,
    city: info.value.city || null,
    introduce: info.value.introduce || null,
    birthday: info.value.birthday || null,
  }
  // 更换了头像则一并提交
  if (iconChanged.value && user.value.icon) {
    data.icon = user.value.icon
  }
  // gender：false=男，true=女；未选择时不传
  if (info.value.gender === true || info.value.gender === false) {
    data.gender = info.value.gender
  }
  userApi
    .updateInfo(data)
    .then(() => {
      // 同步更新本地缓存（昵称、头像也写入 session，供其他页面立即生效）
      const saved = JSON.parse(JSON.stringify(info.value))
      if (iconChanged.value && user.value.icon) {
        saved.icon = user.value.icon
      }
      sessionStorage.setItem('userInfo', JSON.stringify(saved))
      ElMessage.success('保存成功')
      setTimeout(() => router.push('/profile'), 800)
    })
    .catch((err) => ElMessage.error(err))
}
</script>

<style>
/* 资料编辑页基础布局样式
 * 旧 info-edit.html 的 .edit-container/.info-box/.info-item 等类在现有 CSS 中缺失（玻璃化改造时丢失），
 * 此处按原版黑马点评样式补齐，并叠加玻璃拟态主题 */
.edit-container {
  padding: 20px 0;
}
.info-box {
  background: rgba(255, 255, 255, 0.25);
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  padding: 0 15px;
  margin-bottom: 15px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 52px;
  font-size: 14px;
}
.info-label {
  color: var(--text-strong);
  font-weight: 500;
}
.info-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-weak);
}
.info-btn img {
  margin-right: 4px;
  background: #eee;
}
.info-btn .el-input__inner,
.info-btn .el-input__wrapper {
  text-align: right;
}
.divider {
  height: 1px;
  background: rgba(31, 45, 61, 0.08);
}
/* 保存按钮：glass.css 的 .el-button { background: var(--accent) !important } 会把内联样式压成深色，
   此处用更高优先级选择器恢复主题橙 */
.edit-container .el-button--primary {
  background: #ff6633 !important;
  border-color: #ff6633 !important;
}
</style>
