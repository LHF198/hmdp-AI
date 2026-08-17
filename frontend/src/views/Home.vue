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
            <span class="el-dropdown-link city-dropdown">
              {{ city }} <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
          </template>
        </el-popover>
      </div>
      <div class="search-input">
        <el-input
          size="small"
          placeholder="请输入商户名、地点"
          v-model="searchKey"
          @keyup.enter="doSearch"
        >
          <template #prefix>
            <el-icon class="el-input__icon search-icon" @click="doSearch"><Search /></el-icon>
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
      <!-- 首次加载骨架屏：避免白屏闪烁 -->
      <template v-if="loading">
        <div class="blog-skeleton" v-for="i in 4" :key="'sk' + i">
          <div class="blog-skeleton-img"></div>
          <div class="blog-skeleton-line"></div>
          <div class="blog-skeleton-line short"></div>
        </div>
      </template>
      <template v-else>
        <div class="blog-box" v-for="b in blogs" :key="b.id" :style="cardStyle(b)">
          <div class="blog-img" :ref="(el) => registerImgBox(b, el)" @click="toBlogDetail(b)">
            <img v-img-fade :src="b.img" alt="" loading="lazy" @load="onCardImgLoad(b, $event)" />
          </div>
          <div class="blog-title">{{ b.title }}</div>
          <div class="blog-foot">
            <div class="blog-user-icon">
              <img :src="b.icon || '/imgs/icons/default-icon.png'" alt="" />
            </div>
            <div class="blog-user-name">{{ b.name }}</div>
            <div class="blog-liked" @click="addLike(b)">
              <LikeIcon :active="b.isLike" />
              {{ b.liked }}
            </div>
          </div>
        </div>
        <!-- 加载尾巴：触底加载中 / 到底提示 -->
        <div class="blog-list-tail" v-if="blogs.length > 0">
          <span v-if="loadingMore" class="tail-loading">正在加载...</span>
          <span v-else-if="!hasMore">今天的分享就到这里，明天再来看看吧</span>
        </div>
      </template>
    </div>
    <FootBar :active-btn="1" />
  </div>
</template>

<script setup>
// 首页：店铺分类横滑、热门笔记流（触底分页加载）、关键词搜索与城市选择
// （省→市数据复用旧 MPA 的 cities.js，选择结果存 localStorage 供全站使用）
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopApi } from '@/api/shop'
import { blogApi } from '@/api/blog'
// 副作用导入：cities.js 以 ES 模块形式导出全国省→市数据
import { CITY_DATA } from '@/utils/cities'
import LikeIcon from '@/components/LikeIcon.vue'

const router = useRouter()

const types = ref([]) // 类型列表
const blogs = ref([]) // 笔记列表
const current = ref(1) // blog 页码
const isReachBottom = ref(false)
const loading = ref(true) // 首次加载：展示骨架屏
const loadingMore = ref(false) // 触底加载下一页：尾巴展示“正在加载”
const hasMore = ref(true) // 是否还有下一页（后端返回空页即到底）
const searchKey = ref('')
const city = ref(localStorage.getItem('hmdp_city') || '杭州')
const hotCities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '南京', '武汉', '西安', '重庆']
const cityPopVisible = ref(false)
const provinces = ref([])
const curProvince = ref('')
const imgRatios = ref({}) // 每张卡片首图的自然宽高比（width / height），驱动卡片高度自适应

const curCities = computed(() => {
  const p = provinces.value.find((x) => x.name === curProvince.value)
  return p ? p.cities : []
})

onMounted(() => {
  // 加载全国省→市数据
  provinces.value = CITY_DATA.map((p) => ({ name: p.name, cities: p.cities }))
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
      const list = data || []
      list.forEach((b) => {
        b.img = (b.images || '').split(',')[0]
        // 若已有缓存比例则直接应用，否则等 img @load 回调
        if (imgRatios.value[b.id]) {
          b._ratio = imgRatios.value[b.id]
        }
      })
      blogs.value = blogs.value.concat(list)
      // 对已有缓存比例的卡片，DOM 渲染后直接设置高度（@load 可能因缓存不触发）
      nextTick(() => {
        list.forEach((b) => {
          if (b._ratio) {
            const box = imgBoxRefs.get(b.id)
            if (box && box.offsetWidth > 0) {
              box.style.height = Math.round(box.offsetWidth / b._ratio) + 'px'
              box.style.aspectRatio = ''
            }
          }
        })
      })
      // 后端返回空页：已到底，关闭触底分页并展示到底提示
      if (list.length === 0) {
        hasMore.value = false
        current.value--
      }
    })
    .catch((err) => {
      current.value--
      ElMessage.error(err)
    })
    .finally(() => {
      loading.value = false
      loadingMore.value = false
    })
}

function addLike(b) {
  // 乐观更新：立即翻转状态，失败时回退
  b.isLike = !b.isLike
  b.liked += b.isLike ? 1 : -1
  blogApi
    .like(b.id)
    .then(() => {
      // 服务端确认：刷新真实状态
      queryBlogById(b)
    })
    .catch((err) => {
      // 失败回退
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
  if (scrollTop + offsetHeight >= scrollHeight - 1 && !isReachBottom.value) {
    isReachBottom.value = true
    // 已到底或首次加载未完成时不再请求下一页
    if (!hasMore.value || loading.value) {
      return
    }
    // 再次查询下一页数据
    loadingMore.value = true
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

// ===== 卡片图片比例自适应 =====
// 核心思路：图片加载后测量容器实际渲染宽度，按图片自然宽高比计算高度，
// 直接设置 inline height。容器比例与图片完全一致，contain 无留白无裁剪。
const imgBoxRefs = new Map() // blogId → DOM element

function registerImgBox(b, el) {
  if (el) imgBoxRefs.set(b.id, el)
  else imgBoxRefs.delete(b.id)
}

function onCardImgLoad(b, e) {
  const img = e.target
  if (img.naturalWidth && img.naturalHeight) {
    const ratio = img.naturalWidth / img.naturalHeight
    imgRatios.value[b.id] = ratio
    b._ratio = ratio
    // 测量容器实际渲染宽度，按图片比例计算高度
    const box = imgBoxRefs.get(b.id)
    if (box) {
      const w = box.offsetWidth
      if (w > 0) {
        box.style.height = Math.round(w / ratio) + 'px'
        box.style.aspectRatio = '' // 清除 fallback，用精确高度
      }
    }
  }
}

// 图片容器样式：未加载时用 1:1 占位，加载后由 onCardImgLoad 设置精确高度
function imgBoxStyle(b) {
  return {}
}

// 卡片整体样式（预留，当前无额外样式）
function cardStyle(b) {
  return {}
}
</script>

<!-- 城市选择器与搜索栏样式已收敛至 styles/page-home.css 与 styles/layout.css，
     本页无需再声明局部样式（旧实现的 .city-dropdown 白字是为黑色搜索栏适配，
     搜索栏改为玻璃底后已统一为 --text-strong） -->
