<template>
  <div class="blog-edit-page">
    <div class="header">
      <div class="header-cancel-btn" @click="goBack">取消</div>
      <div class="header-title">
        &nbsp;&nbsp;发笔记<el-icon :size="16"><InfoFilled /></el-icon>
      </div>
      <div class="header-commit">
        <div class="header-commit-btn" @click="submitBlog">发布</div>
      </div>
    </div>

    <div class="page-wrapper">
      <div class="content-card upload-box">
        <div class="section-header">
          <div>
            <div class="section-title">图集素材</div>
            <div class="section-subtitle">点击下方上传旅拍或探店照片</div>
          </div>
          <div class="section-pill" v-if="fileList.length">{{ fileList.length }} 张</div>
        </div>
        <input type="file" @change="fileSelected" name="file" ref="fileInput" style="display: none" />
        <div class="upload-btn" @click="openFileDialog">
          <el-icon :size="24"><Camera /></el-icon>
          <div class="upload-tip">上传照片</div>
        </div>
        <div class="pic-list">
          <div class="pic-box" v-for="(f, i) in fileList" :key="i">
            <img :src="f" alt="" />
            <el-icon @click="deletePic(i)"><Close /></el-icon>
          </div>
        </div>
      </div>

      <div class="content-card blog-title">
        <label class="section-title" for="blog-title-input">笔记标题</label>
        <input id="blog-title-input" v-model="params.title" type="text" placeholder="填写标题更容易上首页哦~" />
      </div>

      <div class="content-card blog-content">
        <label class="section-title" for="blog-content-input">正文内容</label>
        <textarea
          id="blog-content-input"
          v-model="params.content"
          placeholder="最近打卡了什么地方，有什么新奇体验呢？"
        ></textarea>
      </div>

      <div class="content-card blog-shop-card" @click="showDialog = true">
        <div class="blog-shop">
          <div class="shop-left">关联商户</div>
          <div v-if="selectedShop.name">{{ selectedShop.name }}</div>
          <div v-else>去选择&nbsp;<el-icon :size="14"><ArrowRight /></el-icon></div>
        </div>
      </div>
    </div>

    <div class="mask" v-show="showDialog" @click="showDialog = false"></div>

    <transition name="el-zoom-in-bottom">
      <div class="shop-dialog" v-show="showDialog">
        <div class="blog-shop">
          <div class="shop-left">关联商户</div>
        </div>
        <div class="search-bar">
          <div class="city-select">
            <el-dropdown trigger="click" @command="selectCity">
              <span class="el-dropdown-link" style="font-size: 13px">
                {{ city }} <el-icon :size="12"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="c in cities" :key="c" :command="c">{{ c }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="search-input">
            <el-icon :size="16" @click="queryShops"><Search /></el-icon>
            <input v-model="shopName" type="text" placeholder="搜索商户名称" />
          </div>
        </div>
        <div class="shop-list">
          <div v-for="s in shops" :key="s.id" class="shop-item" @click="selectShop(s)">
            <div class="shop-name">{{ s.name }}</div>
            <div>{{ s.area }}</div>
          </div>
        </div>
      </div>
    </transition>

    <AiLauncher />
  </div>
</template>

<script setup>
// 笔记发布/编辑页：图文上传、正文编辑、选择关联店铺与城市，发布后跳转笔记详情
// （旧 MPA 页面 blog-edit.html 的 SPA 迁移版）
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { blogApi } from '@/api/blog'
import { shopApi } from '@/api/shop'
import { userApi } from '@/api/user'
import AiLauncher from '@/components/AiLauncher.vue'
// 副作用导入：window.CITY_DATA 城市数据（与旧 MPA 共用）
import '../../html/hmdp/js/cities.js'

const router = useRouter()

const fileList = ref([])
const params = ref({})
const showDialog = ref(false)
const shops = ref([])
const shopName = ref('')
const selectedShop = ref({})
const city = ref('杭州')
const cities = ref([])
const fileInput = ref(null)

onMounted(() => {
  // 读取主页选择的城市
  const c = localStorage.getItem('hmdp_city')
  if (c && c !== '全部') {
    city.value = c
  }
  // 加载完整城市列表（全国省→市数据，去掉省维度后平铺）
  cities.value = (window.CITY_DATA || []).reduce((acc, p) => acc.concat(p.cities), [])
  checkLogin()
  queryShops()
})

function selectCity(c) {
  city.value = c
  queryShops()
}

function queryShops() {
  shopApi
    .ofName({ name: shopName.value, city: city.value })
    .then(({ data }) => (shops.value = data))
    .catch((err) => ElMessage.error(err))
}

function selectShop(s) {
  selectedShop.value = s
  showDialog.value = false
}

function submitBlog() {
  const data = { ...params.value }
  data.images = fileList.value.join(',')
  data.shopId = selectedShop.value.id
  blogApi
    .publish(data)
    .then(() => router.push('/profile'))
    .catch((err) => ElMessage.error(err))
}

function openFileDialog() {
  fileInput.value.click()
}

function fileSelected() {
  const file = fileInput.value.files[0]
  blogApi
    .upload(file)
    .then(({ data }) => fileList.value.push('/imgs' + data))
    .catch((err) => ElMessage.error(err))
}

function deletePic(i) {
  blogApi
    .deleteImage(fileList.value[i])
    .then(() => fileList.value.splice(i, 1))
    .catch((err) => ElMessage.error(err))
}

function checkLogin() {
  const token = sessionStorage.getItem('token')
  if (!token) {
    router.replace('/login')
    return
  }
  userApi
    .me()
    .then(() => {})
    .catch((err) => {
      ElMessage.error(err)
      setTimeout(() => router.replace('/login'), 200)
    })
}

function goBack() {
  router.back()
}
</script>
