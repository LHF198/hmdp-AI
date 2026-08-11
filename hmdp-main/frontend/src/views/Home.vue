<template>
  <div>
    <div class="search-bar">
      <div class="city-btn">
        <el-popover placement="bottom-start" width="320" trigger="click" v-model="cityPopVisible">
          <div class="city-picker">
            <div class="city-picker-head">
              <span class="city-picker-title">选择地区</span>
              <span class="city-picker-all" @click="pickCity('全部')">全部城市</span>
            </div>
            <div class="city-picker-hot">
              <span
                v-for="c in hotCities"
                :key="c"
                class="city-picker-hot-item"
                :class="{ active: city === c }"
                @click="pickCity(c)"
                >{{ c }}</span
              >
            </div>
            <div class="city-picker-cols">
              <div class="city-picker-prov">
                <div
                  v-for="p in provinces"
                  :key="p.name"
                  class="city-picker-prov-item"
                  :class="{ active: p.name === curProvince }"
                  @click="curProvince = p.name"
                  >{{ p.name }}</div
                >
              </div>
              <div class="city-picker-city">
                <div
                  v-for="c in curCities"
                  :key="c"
                  class="city-picker-city-item"
                  :class="{ active: city === c }"
                  @click="pickCity(c)"
                  >{{ c }}</div
                >
              </div>
            </div>
          </div>
          <template #reference>
            <span class="el-dropdown-link" style="color: #fff; font-size: 14px">
              {{ city }} <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
          </template>
        </el-popover>
      </div>
      <div class="search-input">
        <el-input
          size="mini"
          placeholder="请输入商户名、地点"
          v-model="searchKey"
          @keyup.enter="doSearch"
        >
          <template #prefix>
            <el-icon class="el-input__icon" style="cursor: pointer" @click="doSearch"><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="header-icon" @click="toPage(4)">
        <el-icon :size="18"><User /></el-icon>
      </div>
    </div>
    <div class="type-list">
      <div class="type-box" v-for="t in types" :key="t.id" @click="toShopList(t.id, t.name)">
        <div class="type-view"><img :src="'/imgs/' + t.icon" alt="" /></div>
        <div class="type-text">{{ t.name }}</div>
      </div>
    </div>
    <div class="blog-list" @scroll="onScroll">
      <div class="blog-box" v-for="b in blogs" :key="b.id">
        <div class="blog-img" @click="toBlogDetail(b)">
          <img :src="b.img" alt="" loading="lazy" />
        </div>
        <div class="blog-title">{{ b.title }}</div>
        <div class="blog-foot">
          <div class="blog-user-icon">
            <img :src="b.icon || '/imgs/icons/default-icon.png'" alt="" />
          </div>
          <div class="blog-user-name">{{ b.name }}</div>
          <div class="blog-liked" @click="addLike(b)">
            <svg
              t="1646634642977"
              class="icon"
              viewBox="0 0 1024 1024"
              version="1.1"
              xmlns="http://www.w3.org/2000/svg"
              p-id="2187"
              width="14"
              height="14"
            >
              <path
                d="M160 944c0 8.8-7.2 16-16 16h-32c-26.5 0-48-21.5-48-48V528c0-26.5 21.5-48 48-48h32c8.8 0 16 7.2 16 16v448zM96 416c-53 0-96 43-96 96v416c0 53 43 96 96 96h96c17.7 0 32-14.3 32-32V448c0-17.7-14.3-32-32-32H96zM505.6 64c16.2 0 26.4 8.7 31 13.9 4.6 5.2 12.1 16.3 10.3 32.4l-23.5 203.4c-4.9 42.2 8.6 84.6 36.8 116.4 28.3 31.7 68.9 49.9 111.4 49.9h271.2c6.6 0 10.8 3.3 13.2 6.1s5 7.5 4 14l-48 303.4c-6.9 43.6-29.1 83.4-62.7 112C815.8 944.2 773 960 728.9 960h-317c-33.1 0-59.9-26.8-59.9-59.9v-455c0-6.1 1.7-12 5-17.1 69.5-109 106.4-234.2 107-364h41.6z m0-64h-44.9C427.2 0 400 27.2 400 60.7c0 127.1-39.1 251.2-112 355.3v484.1c0 68.4 55.5 123.9 123.9 123.9h317c122.7 0 227.2-89.3 246.3-210.5l47.9-303.4c7.8-49.4-30.4-94.1-80.4-94.1H671.6c-50.9 0-90.5-44.4-84.6-95l23.5-203.4C617.7 55 568.7 0 505.6 0z"
                p-id="2188"
                :fill="b.isLike ? '#ff6633' : '#82848a'"
              ></path>
            </svg>
            {{ b.liked }}
          </div>
        </div>
      </div>
    </div>
    <FootBar :active-btn="1" />
  </div>
</template>

<script setup>
// 首页：店铺分类横滑、热门笔记流（触底分页加载）、关键词搜索与城市选择
// （省→市数据复用旧 MPA 的 cities.js，选择结果存 localStorage 供全站使用）
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopApi } from '@/api/shop'
import { blogApi } from '@/api/blog'
// 副作用导入：cities.js 以 window.CITY_DATA 形式暴露全国省→市数据（与旧 MPA 共用同一份数据源）
import '../../html/hmdp/js/cities.js'

const router = useRouter()

const types = ref([]) // 类型列表
const blogs = ref([]) // 笔记列表
const current = ref(1) // blog 页码
const isReachBottom = ref(false)
const searchKey = ref('')
const city = ref(localStorage.getItem('hmdp_city') || '杭州')
const hotCities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '南京', '武汉', '西安', '重庆']
const cityPopVisible = ref(false)
const provinces = ref([])
const curProvince = ref('')

const curCities = computed(() => {
  const p = provinces.value.find((x) => x.name === curProvince.value)
  return p ? p.cities : []
})

onMounted(() => {
  // 加载全国省→市数据
  provinces.value = (window.CITY_DATA || []).map((p) => ({ name: p.name, cities: p.cities }))
  curProvince.value = provinces.value.length ? provinces.value[0].name : ''
  queryTypes()
  queryHotBlogsScroll()
})

function queryTypes() {
  shopApi
    .types()
    .then(({ data }) => {
      types.value = data
    })
    .catch((err) => ElMessage.error(err))
}

function pickCity(c) {
  // 切换城市并持久化，供店铺列表/地图/发笔记等页面联动
  city.value = c
  localStorage.setItem('hmdp_city', c)
  cityPopVisible.value = false
  ElMessage.success(c === '全部' ? '已切换到全部城市' : '已切换到' + c)
}

function queryHotBlogsScroll() {
  blogApi
    .hot(current.value)
    .then(({ data }) => {
      data.forEach((b) => (b.img = b.images.split(',')[0]))
      blogs.value = blogs.value.concat(data)
    })
    .catch((err) => ElMessage.error(err))
}

function addLike(b) {
  blogApi
    .like(b.id)
    .then(() => {
      queryBlogById(b)
    })
    .catch((err) => ElMessage.error(err))
}

function queryBlogById(b) {
  blogApi
    .get(b.id)
    .then(({ data }) => {
      b.liked = data.liked
      b.isLike = data.isLike
    })
    .catch(() => {
      // 刷新失败时本地先 +1，避免点赞状态丢失
      b.liked++
    })
}

function onScroll(e) {
  const scrollTop = e.target.scrollTop
  const offsetHeight = e.target.offsetHeight
  const scrollHeight = e.target.scrollHeight
  if (scrollTop + offsetHeight >= scrollHeight - 1 && !isReachBottom.value) {
    isReachBottom.value = true
    // 再次查询下一页数据
    current.value++
    queryHotBlogsScroll()
  } else {
    isReachBottom.value = false
  }
}

function toShopList(id, name) {
  router.push({ path: '/shops', query: { type: id, name } })
}

function doSearch() {
  // 首页搜索：跳转店铺列表页的搜索模式
  const key = (searchKey.value || '').trim()
  if (!key) {
    ElMessage.info('请输入搜索关键词')
    return
  }
  router.push({ path: '/shops', query: { search: key } })
}

function toBlogDetail(b) {
  router.push('/blog/' + b.id)
}

function toPage(i) {
  // 右上角头像：跳转个人主页（与底部导航保持一致）
  if (i === 0) {
    router.push('/blog/edit')
  } else if (i === 1) {
    router.push('/')
  } else if (i === 2) {
    router.push('/map')
  } else if (i === 3) {
    router.push('/message')
  } else if (i === 4) {
    router.push('/profile')
  }
}
</script>

<style>
/* 省→市两级地区选择器（从旧 index.html 内联样式迁移） */
.city-picker {
  width: 100%;
}
.city-picker-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.city-picker-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.city-picker-all {
  font-size: 13px;
  color: #ff6633;
  cursor: pointer;
}
.city-picker-hot {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 0 8px;
  border-bottom: 1px solid #f0f0f0;
}
.city-picker-hot-item {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: #f5f5f5;
  color: #555;
  cursor: pointer;
}
.city-picker-hot-item.active {
  background: #fff0eb;
  color: #ff6633;
}
.city-picker-cols {
  display: flex;
  height: 320px;
  margin-top: 8px;
}
.city-picker-prov {
  width: 96px;
  flex-shrink: 0;
  overflow-y: auto;
  border-right: 1px solid #f0f0f0;
}
.city-picker-prov-item {
  padding: 8px 10px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  border-radius: 8px;
}
.city-picker-prov-item.active {
  background: #fff0eb;
  color: #ff6633;
  font-weight: 600;
}
.city-picker-city {
  flex: 1;
  overflow-y: auto;
  padding-left: 8px;
}
.city-picker-city-item {
  padding: 8px 10px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  border-radius: 8px;
}
.city-picker-city-item.active {
  background: #fff0eb;
  color: #ff6633;
  font-weight: 600;
}
</style>
