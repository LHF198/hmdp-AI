<template>
  <div class="map-page">
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="24"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">地图找店&nbsp;&nbsp;&nbsp;</div>
    </div>

    <div class="map-types">
      <div class="map-type city-type">
        <el-dropdown trigger="click" @command="selectCity">
          <span class="city-label">
            {{ city }} <el-icon :size="12"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="全部">全部</el-dropdown-item>
              <el-dropdown-item v-for="c in cities" :key="c" :command="c">{{ c }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div
        class="map-type"
        :class="{ active: activeType === 0 }"
        @click="filterByType(0)"
      >
        全部
      </div>
      <div
        v-for="t in types"
        :key="t.id"
        class="map-type"
        :class="{ active: activeType === t.id }"
        @click="filterByType(t.id)"
      >
        {{ t.name }}
      </div>
    </div>

    <div class="map-canvas">
      <!-- 百度地图容器 -->
      <div class="map-container" id="mapContainer"></div>
      <div class="map-hint">百度地图 · 拖拽/滚轮缩放 · 点击标记查看店铺</div>

      <!-- 加载遮罩 -->
      <div v-if="loading" class="map-loading">
        <el-icon class="is-loading" :size="16"><Loading /></el-icon>&nbsp;加载中...
      </div>

      <!-- AK 缺失 / 加载失败提示 -->
      <div v-if="mapLoadError" class="map-error">
        <el-icon :size="22" color="#e6a23c"><WarningFilled /></el-icon>
        <div style="margin-top: 4px">{{ mapLoadError }}</div>
        <div class="map-error-sub">
          请在 Map.vue 顶部配置百度地图 AK：<br />
          https://lbsyun.baidu.com/apiconsole/key 申请（浏览器端），<br />
          并把 Referer 白名单加上 127.0.0.1:8080/* 与 localhost:8080/*
        </div>
      </div>

      <!-- 选中的店铺信息卡 -->
      <div v-if="activeShop" class="shop-card" @click="toDetail(activeShop.id)">
        <div class="card-close" @click.stop="activeShop = null">
          <el-icon :size="14"><Close /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-title">{{ activeShop.name }}</div>
          <div class="card-sub">
            均价￥{{ activeShop.avgPrice || '-' }}/人 · 距观察点约{{ distanceText }}
          </div>
          <div class="card-sub">
            <el-icon :size="13"><MapLocation /></el-icon>
            {{ activeShop.address }}
          </div>
        </div>
        <div class="card-arrow">
          <el-icon :size="18"><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 空态 -->
      <div v-if="!loading && shops.length === 0" class="map-empty">
        <el-icon :size="48" color="#ddd"><MapLocation /></el-icon><br />
        该分类下暂无店铺
      </div>
    </div>

    <FootBar :active-btn="2" />
  </div>
</template>

<script setup>
// 地图找店：BMapGL 全量标记 + 城市/分类过滤 + 信息窗/底部店铺卡；
// 数据来自 /shop/map/list（GCJ-02 坐标，渲染时转 BD-09）（旧 MPA 页面 map.html 的 SPA 迁移版）
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopApi } from '@/api/shop'
import { useCityStore } from '@/stores/city'

// ★★ 百度地图 AK 配置（与旧 MPA map.html 共用同一个浏览器端 AK，Referer 白名单需含 8080）★★
const BAIDU_MAP_AK = 'Zvsh9Bo1IPj673vg707O9DFX6smMDYyA'

const router = useRouter()
const cityStore = useCityStore()

const loading = ref(true)
const types = ref([])
const allShops = ref([]) // 全部店铺
const shops = ref([]) // 当前过滤后的店铺
const activeType = ref(0)
const activeShop = ref(null)
const city = ref(cityStore.city || '杭州')
const cities = ref([])
const mapReady = ref(false)
const mapLoadError = ref('')
let map = null // BMapGL 实例

// 默认观察点（杭州市区），用于计算距离
const centerX = 120.15
const centerY = 30.32

// GCJ-02（高德/腾讯）→ BD-09（百度）坐标转换
function gcj02ToBd09(lng, lat) {
  const xPi = (Math.PI * 3000.0) / 180.0
  const z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * xPi)
  const theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * xPi)
  return {
    lng: z * Math.cos(theta) + 0.0065,
    lat: z * Math.sin(theta) + 0.006,
  }
}

// 动态加载百度地图脚本（带回调）
function loadBMap(ak) {
  return new Promise((resolve, reject) => {
    if (window.BMapGL) {
      resolve()
      return
    }
    window.__onBMapLoaded = () => resolve()
    const s = document.createElement('script')
    // 2026 年起百度已下线 v=3.0 旧版 JSAPI（错误码 8001），必须使用 GL 版（type=webgl），API 用法不变
    s.src = 'https://api.map.baidu.com/api?v=1.0&type=webgl&ak=' + ak + '&callback=__onBMapLoaded'
    s.onerror = () => reject(new Error('load failed'))
    document.head.appendChild(s)
  })
}

// 自定义橙色大头针图标
function makePinIcon() {
  const svg =
    '<svg xmlns="http://www.w3.org/2000/svg" width="30" height="42" viewBox="0 0 30 42">' +
    '<path d="M15 0C6.7 0 0 6.7 0 15c0 11.2 15 27 15 27s15-15.8 15-27C30 6.7 23.3 0 15 0z" fill="#ff6633" stroke="#fff" stroke-width="2"/>' +
    '<circle cx="15" cy="15" r="6" fill="#fff"/></svg>'
  return new BMapGL.Icon('data:image/svg+xml,' + encodeURIComponent(svg), new BMapGL.Size(30, 42), {
    anchor: new BMapGL.Size(15, 42),
  })
}

function initMap() {
  map = new BMapGL.Map('mapContainer')
  map.centerAndZoom(new BMapGL.Point(120.15, 30.32), 13)
  map.enableScrollWheelZoom(true)
  map.addControl(new BMapGL.ZoomControl())
  map.addControl(new BMapGL.ScaleControl())
  mapReady.value = true
  renderShops()
}

function queryTypes() {
  shopApi
    .types()
    .then(({ data }) => {
      types.value = data || []
    })
    .catch((err) => ElMessage.error(err))
}

function queryShops() {
  shopApi
    .mapList()
    .then(({ data }) => {
      allShops.value = data || []
      applyCity()
    })
    .catch((err) => {
      ElMessage.error(err)
    })
    .finally(() => {
      loading.value = false
    })
}

function queryCities() {
  shopApi
    .cities()
    .then(({ data }) => (cities.value = data || []))
    .catch(() => {})
}

function selectCity(c) {
  city.value = c
  cityStore.setCity(c)
  applyCity()
}

// 按当前城市过滤并重渲染地图
function applyCity() {
  activeShop.value = null
  shops.value =
    city.value === '全部' || !city.value
      ? allShops.value
      : allShops.value.filter((s) => s.city === city.value)
  renderShops()
}

function filterByType(typeId) {
  activeType.value = typeId
  activeShop.value = null
  const base =
    city.value === '全部' || !city.value
      ? allShops.value
      : allShops.value.filter((s) => s.city === city.value)
  shops.value = typeId ? base.filter((s) => s.typeId === typeId) : base
  renderShops()
}

// 把当前店铺列表渲染到百度地图上
function renderShops() {
  if (!mapReady.value || !map) {
    return
  }
  map.clearOverlays()
  const pts = []
  const icon = makePinIcon()
  shops.value.forEach((s) => {
    if (s.x == null || s.y == null || isNaN(s.x) || isNaN(s.y)) {
      return
    }
    // 高德坐标（GCJ-02）→ 百度坐标（BD-09）
    const b = gcj02ToBd09(s.x, s.y)
    const pt = new BMapGL.Point(b.lng, b.lat)
    pts.push(pt)
    const marker = new BMapGL.Marker(pt, { icon })
    const label = new BMapGL.Label(s.name, {
      position: pt,
      offset: new BMapGL.Size(20, -12),
    })
    label.setStyle({
      color: '#333',
      fontSize: '11px',
      padding: '2px 6px',
      background: 'rgba(255,255,255,0.95)',
      border: '1px solid #eee',
      borderRadius: '4px',
      boxShadow: '0 1px 4px rgba(0,0,0,0.15)',
      whiteSpace: 'nowrap',
      maxWidth: '140px',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
    })
    marker.setLabel(label)
    marker.addEventListener('click', () => openInfo(s, pt))
    label.addEventListener('click', () => openInfo(s, pt))
    map.addOverlay(marker)
  })
  // 视野自适应：1 家店放大聚焦，多家店自动框选
  if (pts.length === 1) {
    map.centerAndZoom(pts[0], 16)
  } else if (pts.length > 1) {
    map.setViewport(pts, { zoomFactor: 0.8, delay: 100 })
  } else {
    map.centerAndZoom(new BMapGL.Point(120.15, 30.32), 13)
  }
}

// 点击标记：打开信息窗口 + 显示底部卡片
function openInfo(s, pt) {
  activeShop.value = s
  const html =
    '<div style="min-width:200px;font-family:Microsoft YaHei,sans-serif">' +
    '<div style="font-size:15px;font-weight:700;color:#1a1a1a;margin-bottom:4px">' +
    s.name +
    '</div>' +
    '<div style="font-size:12px;color:#999">均价￥' +
    (s.avgPrice || '-') +
    '/人 · ' +
    (s.address || '') +
    '</div>' +
    '<div style="margin-top:8px;text-align:right">' +
    '<span onclick="__openShopDetail(' +
    s.id +
    ')" style="color:#ff6633;font-size:13px;cursor:pointer">查看详情 ›</span>' +
    '</div></div>'
  const info = new BMapGL.InfoWindow(html, { width: 240 })
  map.openInfoWindow(info, pt)
}

function toDetail(id) {
  router.push('/shop/' + id)
}

const distanceText = computed(() => {
  if (!activeShop.value || activeShop.value.x == null) {
    return '-'
  }
  // Haversine 近似计算与观察点的距离
  const R = 6371000
  const dx = (activeShop.value.x - centerX) * (Math.PI / 180) * Math.cos(centerY * (Math.PI / 180))
  const dy = (activeShop.value.y - centerY) * (Math.PI / 180)
  const d = Math.round(Math.sqrt(dx * dx + dy * dy) * R)
  if (d < 1000) {
    return d + 'm'
  }
  return (d / 1000).toFixed(1) + 'km'
})

function goBack() {
  router.back()
}

onMounted(() => {
  // 供 InfoWindow 内"查看详情"跳转（闭包持有 router，页面卸载后自动失效）
  window.__openShopDetail = (id) => router.push('/shop/' + id)
  queryTypes()
  queryShops()
  queryCities()
  // 加载百度地图
  if (!BAIDU_MAP_AK) {
    mapLoadError.value = '未配置百度地图 AK'
  } else {
    loadBMap(BAIDU_MAP_AK)
      .then(() => initMap())
      .catch(() => {
        mapLoadError.value = '百度地图脚本加载失败，请检查网络或 AK 是否有效'
      })
  }
})

onBeforeUnmount(() => {
  // SPA 路由切换时销毁地图实例，避免重复初始化/内存泄漏
  if (map) {
    try {
      map.destroy()
    } catch (e) {
      /* 忽略销毁异常 */
    }
    map = null
  }
  delete window.__openShopDetail
})
</script>

<style scoped>
.map-page {
  height: 100vh; /* 固定视口高度：flex 布局内部消化，保证子项百分比/flex 高度可解析 */
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 顶部类型选择条 */
.map-types {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  overflow-x: auto;
  background: rgba(255, 255, 255, 0.85);
  position: relative;
  z-index: 5;
  flex-shrink: 0;
}
.map-types::-webkit-scrollbar {
  display: none;
}
.map-type {
  flex-shrink: 0;
  padding: 5px 14px;
  border-radius: 16px;
  font-size: 13px;
  color: #555;
  background: #f5f5f5;
  cursor: pointer;
}
.map-type.active {
  background: #ff6633;
  color: #fff;
}
.city-type {
  display: flex;
  align-items: center;
  background: #ffe8e0;
  color: #ff6633;
}
.city-label {
  color: #ff6633;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

/* 地图画布：flex 占满剩余高度，底部留 8vh 给悬浮 FootBar（与全局 .foot height: 8% 对齐）
 * flex-basis: 0 + flex-grow 可让本元素获得“确定高度”，
 * 从而子元素 .map-container 的 height: 100% 能正确解析（仅 flex: 1 时会塔陷为 0） */
.map-canvas {
  position: relative;
  flex: 1 1 0;
  min-height: 0;
  margin-bottom: 8vh;
  overflow: hidden;
  background: #e8e8e8;
}
.map-container {
  width: 100%;
  height: 100%;
}
.map-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  color: #999;
  font-size: 14px;
  z-index: 20;
}
.map-error {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.97);
  padding: 18px 26px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  color: #666;
  font-size: 14px;
  text-align: center;
  z-index: 15;
  width: 72%;
}
.map-error-sub {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  line-height: 1.7;
}
.map-hint {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 11px;
  border-radius: 12px;
  padding: 3px 12px;
  z-index: 5;
}

/* 店铺信息卡 */
.shop-card {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 14px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 14px;
  padding: 14px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.shop-card .card-info {
  flex: 1;
  min-width: 0;
}
.shop-card .card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shop-card .card-sub {
  font-size: 12px;
  color: #999;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shop-card .card-sub .el-icon {
  vertical-align: -2px;
}
.shop-card .card-arrow {
  color: #bbb;
  font-size: 18px;
}
.shop-card .card-close {
  position: absolute;
  right: 8px;
  top: 6px;
  color: #ccc;
  font-size: 14px;
  cursor: pointer;
  padding: 2px;
}
.map-empty {
  position: absolute;
  left: 0;
  right: 0;
  top: 45%;
  transform: translateY(-50%);
  text-align: center;
  color: #999;
  font-size: 14px;
  z-index: 4;
}
</style>
