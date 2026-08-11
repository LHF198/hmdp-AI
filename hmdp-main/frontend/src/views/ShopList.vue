<template>
  <div>
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="24"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">{{ typeName }}</div>
      <div class="header-search" @click="openSearch">
        <el-icon :size="18"><Search /></el-icon>
      </div>
    </div>
    <div class="sort-bar">
      <div class="sort-item">{{ typeName }}</div>
      <div
        class="sort-item sort-dropdown"
        v-for="item in sortItems"
        :key="item.field"
        :class="{ 'sort-active': params.sortBy === item.field }"
      >
        <span class="sort-label" @click.stop="toggleSortMenu(item.field)">
          {{ item.label }}
          <el-icon v-if="params.sortBy === item.field" :size="12">
            <CaretTop v-if="params.isAsc" /><CaretBottom v-else />
          </el-icon>
          <el-icon v-else :size="12"><ArrowDown /></el-icon>
        </span>
        <div class="sort-menu" v-if="openSort === item.field" @click.stop>
          <div
            class="sort-menu-item"
            :class="{ 'sort-menu-active': params.sortBy === item.field && params.isAsc }"
            @click="applySort(item.field, true)"
          >
            {{ item.ascLabel }}
          </div>
          <div
            class="sort-menu-item"
            :class="{ 'sort-menu-active': params.sortBy === item.field && !params.isAsc }"
            @click="applySort(item.field, false)"
          >
            {{ item.descLabel }}
          </div>
        </div>
      </div>
    </div>
    <div class="shop-list" @scroll="onScroll">
      <div
        v-if="loaded && shops.length === 0"
        style="text-align: center; color: #82848a; padding: 60px 0; font-size: 14px; width: 100%"
      >
        当前城市暂无相关商铺，换个城市或关键词试试吧
      </div>
      <div class="shop-box" v-for="s in shops" :key="s.id" @click="toDetail(s.id)">
        <div class="shop-img"><img :src="s.images" alt="" /></div>
        <div class="shop-info">
          <div class="shop-title shop-item">{{ s.name }}</div>
          <div class="shop-rate shop-item">
            <el-rate disabled :model-value="s.score / 10" text-color="#F63" show-score></el-rate>
            <span>{{ s.comments }}条</span>
          </div>
          <div class="shop-area shop-item">
            <span>{{ s.area }}</span>
            <span v-if="s.distance"
              >{{ s.distance < 1000 ? s.distance.toFixed(1) + 'm' : (s.distance / 1000).toFixed(1) + 'km' }}</span
            >
          </div>
          <div class="shop-avg shop-item">￥{{ s.avgPrice }}/人</div>
          <div class="shop-address shop-item">
            <el-icon :size="14"><MapLocation /></el-icon>
            <span>{{ s.address }}</span>
          </div>
        </div>
      </div>
    </div>
    <AiLauncher />
  </div>
</template>

<script setup>
// 店铺列表页：按分类/关键词查询，支持距离、人气、评分排序与附近（GEO）切换，触底分页加载；
// 路由参数 typeId/typeName 驱动（旧 MPA 页面 shop-list.html 的 SPA 迁移版）
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shopApi } from '@/api/shop'
import AiLauncher from '@/components/AiLauncher.vue'

const route = useRoute()
const router = useRouter()

const shops = ref([]) // 商店列表
const loaded = ref(false) // 首次查询是否完成（用于空态展示）
const isReachBottom = ref(false)
const typeName = ref('')
const openSort = ref('') // 当前展开的排序下拉项
const searchKey = ref('') // 搜索关键词（非空时走名称搜索模式）
const sortItems = [
  { field: 'distance', label: '距离', ascLabel: '距离由近到远', descLabel: '距离由远到近' },
  { field: 'comments', label: '人气', ascLabel: '人气由低到高', descLabel: '人气由高到低' },
  { field: 'score', label: '评分', ascLabel: '评分由低到高', descLabel: '评分由高到低' },
]
const params = ref({
  typeId: 0,
  current: 1,
  sortBy: '',
  isAsc: true,
  x: 120.149993, // 经度
  y: 30.334229, // 纬度
})

// 点击页面空白处收起排序下拉
let closeSortMenu = null

onMounted(() => {
  // 获取参数（旧 MPA 的 URL query 语义：type/name/search）
  loadFromQuery()
  closeSortMenu = () => {
    openSort.value = ''
  }
  document.addEventListener('click', closeSortMenu)
})

// SPA 内同路由 query 变化（如页内搜索跳转）时重新加载，避免复用组件不刷新
watch(
  () => JSON.stringify(route.query),
  () => loadFromQuery()
)

function loadFromQuery() {
  // 重置分页与排序状态
  params.value.current = 1
  params.value.sortBy = ''
  params.value.isAsc = true
  shops.value = []
  isReachBottom.value = false
  loaded.value = false
  params.value.typeId = route.query.type
  searchKey.value = decodeURIComponent((route.query.search || '').toString())
  if (searchKey.value) {
    // 搜索模式：标题展示搜索词
    typeName.value = '搜索：' + searchKey.value
  } else {
    typeName.value = (route.query.name || '').toString()
  }
  // 读取主页选择的城市
  const city = localStorage.getItem('hmdp_city')
  if (city && city !== '全部') {
    params.value.city = city
  }
  // 查询商店
  queryShops()
}

onBeforeUnmount(() => {
  document.removeEventListener('click', closeSortMenu)
})

function queryShops() {
  // 搜索模式走名称模糊搜索接口，否则按类型查询
  let url = 'ofType'
  let p = params.value
  // 非距离排序不走GEO，不传坐标
  if (params.value.sortBy && params.value.sortBy !== 'distance') {
    p = {
      typeId: params.value.typeId,
      current: params.value.current,
      city: params.value.city,
      sortBy: params.value.sortBy,
      // 转字符串：http.js 的 paramsSerializer 会过滤 falsy 值
      isAsc: String(params.value.isAsc),
    }
  }
  if (searchKey.value) {
    url = 'ofName'
    p = {
      name: searchKey.value,
      current: params.value.current,
      city: params.value.city,
      sortBy: params.value.sortBy === 'distance' ? '' : params.value.sortBy,
      isAsc: String(params.value.isAsc),
    }
  }
  shopApi[url](p)
    .then(({ data }) => {
      loaded.value = true
      if (!data) {
        return
      }
      data.forEach((s) => (s.images = s.images.split(',')[0]))
      shops.value = shops.value.concat(data)
    })
    .catch((err) => {
      loaded.value = true
      console.log(err)
      ElMessage.error(err)
    })
}

function openSearch() {
  // 顶部搜索入口：输入关键词后进入搜索模式
  ElMessageBox.prompt('搜索商户名、地点', '搜索', {
    confirmButtonText: '搜索',
    cancelButtonText: '取消',
    inputValue: searchKey.value || '',
  })
    .then(({ value }) => {
      if (value && value.trim()) {
        router.push({ path: '/shops', query: { search: value.trim() } })
      }
    })
    .catch(() => {})
}

function toggleSortMenu(field) {
  openSort.value = openSort.value === field ? '' : field
}

function applySort(field, isAsc) {
  // 切换排序后重新从第一页查询，并清空已有列表
  params.value.sortBy = field
  params.value.isAsc = isAsc
  params.value.current = 1
  shops.value = []
  openSort.value = ''
  isReachBottom.value = false
  queryShops()
}

function goBack() {
  router.back()
}

function toDetail(id) {
  router.push('/shop/' + id)
}

function onScroll(e) {
  const scrollTop = e.target.scrollTop
  const offsetHeight = e.target.offsetHeight
  const scrollHeight = e.target.scrollHeight
  if (scrollTop + offsetHeight + 1 > scrollHeight && !isReachBottom.value) {
    isReachBottom.value = true
    params.value.current++
    queryShops()
  } else {
    isReachBottom.value = false
  }
}
</script>
