<template>
  <div class="shop-detail-page">
    <!-- 页面头部：返回 / 标题 / 订单入口 / 分享 -->
    <div class="header">
      <div class="header-back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title"></div>
      <div class="header-share header-order-btn" @click="showOrders">
        订单
      </div>
      <div class="header-share" title="复制店铺链接分享" @click="share">...</div>
    </div>

    <!-- 店铺不存在/加载失败时的错误空态（避免渲染“暂无店名 + 0 分”的残缺假页面） -->
    <div class="detail-body" v-if="loadError">
      <EmptyState size="roomy">
        <div class="shop-missing">
          <div class="shop-missing-text">店铺不存在或已删除</div>
          <div class="shop-missing-btn" @click="router.push('/')">返回首页</div>
        </div>
      </EmptyState>
    </div>

    <template v-else>
    <!-- 商店信息卡片 -->
    <div class="shop-info-box">
      <div class="shop-title">{{ shop.name || '店铺名称' }}</div>

      <div class="shop-rate" v-if="shop.score">
        <el-rate disabled :model-value="shop.score / 10" :text-color="RATE_TEXT_COLOR" show-score></el-rate>
        <span>{{ shop.comments || 0 }}条</span>
      </div>

      <div class="shop-rate-info" v-if="shop.score">口味:{{ (shop.score / 10).toFixed(1) }} 环境:{{ (shop.score / 10).toFixed(1) }} 服务:{{ (shop.score / 10).toFixed(1) }}</div>

      <!-- 商店图片横向滚动 -->
      <div class="shop-images" v-if="shop.images && shop.images.length > 0">
        <div v-for="(s, i) in shop.images" :key="i">
          <img :src="s" alt="" />
        </div>
      </div>

      <div class="shop-address" v-if="shop.address">
        <div><el-icon :size="16" :color="INFO_ICON_COLOR"><Location /></el-icon></div>
        <span>{{ shop.address }}</span>
      </div>

      <!-- 商店服务标签（静态演示图标） -->
      <div class="shop-service">
        <div class="shop-service-sep">|</div>
        <div class="shop-service-icon">
          <img
            src="https://p0.meituan.net/travelcube/bf684aa196c870810655e45b1e52ce843484.png@24w_16h_40q"
            alt=""
          />
        </div>
        <div>
          <img
            src="https://p0.meituan.net/travelcube/9277ace32123e0c9f59dedf4407892221566.png@24w_24h_40q"
            alt=""
          />
        </div>
      </div>

      <!-- 营业时间 -->
      <div class="shop-open-time" v-if="shop.openHours">
        <span><el-icon :size="16" :color="INFO_ICON_COLOR"><Watch /></el-icon></span>
        <div>营业时间</div>
        <div>{{ shop.openHours }}</div>
        <span class="line-right" @click="shopDetailVisible = true"
          >查看详情 <el-icon :size="12"><ArrowRight /></el-icon
        ></span>
      </div>
    </div>

    <!-- 代金券卡片 -->
    <div class="shop-voucher" v-if="vouchers.length > 0">
      <div class="voucher-header">
        <span class="voucher-icon">券</span>
        <span class="voucher-title-text">代金券</span>
      </div>
      <!-- 未到结束时间的代金券列表（Vue 3 中 v-if 优先于 v-for，不能同元素使用，提前 computed 过滤） -->
      <div class="voucher-box" v-for="v in activeVouchers" :key="v.id">
        <div class="voucher-circle">
          <div class="voucher-b"></div>
          <div class="voucher-b"></div>
          <div class="voucher-b"></div>
        </div>
        <div class="voucher-left">
          <div class="voucher-title">{{ v.title }}</div>
          <div class="voucher-subtitle">{{ v.subTitle }}</div>
          <div class="voucher-price">
            <div>￥{{ formatPrice(v.payValue) }}</div>
            <span>{{ ((v.payValue * 10) / v.actualValue).toFixed(1) }}折</span>
          </div>
        </div>
        <div class="voucher-right">
          <!-- 秒杀类型代金券 -->
          <div v-if="v.type" class="seckill-box">
            <div
              class="voucher-btn"
              :class="{ 'disable-btn': isNotBegin(v) || v.stock < 1 || seckilling }"
              @click="seckill(v)"
            >
              {{ seckilling ? '抢购中...' : '限时抢购' }}
            </div>
            <div class="seckill-stock">剩余 <span>{{ v.stock }}</span> 张</div>
            <div class="seckill-time">{{ countdownText(v) }}</div>
          </div>
          <!-- 普通代金券 -->
          <div class="voucher-btn" v-else>抢购</div>
        </div>
      </div>
    </div>

    <!-- 评论卡片：店铺评价功能尚未开放，聚合展示大家的探店笔记作为真实评价内容 -->
    <div class="shop-comments">
      <div class="comments-head">
        <div>网友评价</div>
        <span v-if="shopBlogs.length > 0" class="comments-sub">来自探店笔记</span>
      </div>
      <div class="shop-blog-strip" v-if="shopBlogs.length > 0">
        <div
          class="shop-blog-card"
          v-for="b in shopBlogs"
          :key="b.id"
          @click="router.push('/blog/' + b.id)"
        >
          <div class="shop-blog-img">
            <img :src="b.img" alt="" loading="lazy" />
          </div>
          <div class="shop-blog-title">{{ b.title }}</div>
          <div class="shop-blog-foot">
            <img class="shop-blog-avatar" :src="b.icon || '/imgs/icons/default-icon.png'" alt="" />
            <span class="shop-blog-name">{{ b.name }}</span>
            <span class="shop-blog-liked">
              <LikeIcon :active="b.isLike" />
              {{ b.liked }}
            </span>
          </div>
        </div>
      </div>
      <EmptyState v-else size="compact" text="暂无评价，来做第一个探店笔记吧" />
    </div>

    <div class="copyright">copyright ©{{ year }} hmdp.com</div>
    </template>

    <!-- 店铺详情弹窗 -->
    <el-dialog title="店铺详情" v-model="shopDetailVisible" width="90%" append-to-body>
      <div class="dialog-detail">
        <div>店铺名称：{{ shop.name || '-' }}</div>
        <div v-if="shop.area">商圈：{{ shop.area }}</div>
        <div v-if="shop.address">地址：{{ shop.address }}</div>
        <div v-if="shop.avgPrice">人均：￥{{ shop.avgPrice }}</div>
        <div v-if="shop.openHours">营业时间：{{ shop.openHours }}</div>
        <div v-if="shop.score">综合评分：{{ (shop.score / 10).toFixed(1) }} 分</div>
        <div v-if="shop.comments">评价数：{{ shop.comments }} 条</div>
        <div v-if="shop.sold">销量：{{ shop.sold }} 单</div>
      </div>
    </el-dialog>

    <!-- 我的秒杀订单弹窗 -->
    <el-dialog title="我的秒杀订单" v-model="ordersVisible" width="90%" append-to-body>
      <EmptyState v-if="orders.length === 0" size="compact" text="暂无订单" />
      <div
        v-for="o in orders"
        :key="o.id"
        class="order-item"
      >
        <div class="order-title">
          {{ o.voucherTitle || '代金券 ' + o.voucherId }}
        </div>
        <div class="order-meta">
          订单号：{{ o.id }}
          <span v-if="o.payValue"> | ￥{{ formatPrice(o.payValue) }}</span>
          | {{ orderStatus(o.status) }}
        </div>
        <div class="order-time">
          下单时间：{{ formatTime(o.createTime) }}
        </div>
        <div class="order-actions">
          <el-button
            v-if="o.status === ORDER_STATUS.UNPAID"
            size="small"
            type="danger"
            round
            @click="payOrder(o)"
          >模拟支付</el-button>
          <span
            v-if="o.status === ORDER_STATUS.PAID || o.status === ORDER_STATUS.USED"
            class="order-code"
          >核销码：{{ o.id }}</span>
        </div>
      </div>
    </el-dialog>

    <AiLauncher />
  </div>
</template>

<script setup>
// 店铺详情页：展示店铺信息、代金券与秒杀倒计时，支持限时抢购、查看我的秒杀订单；
// 路由参数 id 驱动数据加载（旧 MPA 页面 shop-detail.html 的 SPA 迁移版）
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopApi } from '@/api/shop'
import { blogApi } from '@/api/blog'
import { voucherApi } from '@/api/voucher'
import { ORDER_STATUS, ORDER_STATUS_TEXT } from '@/utils/order-status'
import { INFO_ICON_COLOR, RATE_TEXT_COLOR } from '@/utils/colors'
import { useUserStore } from '@/stores/user'
import AiLauncher from '@/components/AiLauncher.vue'
import LikeIcon from '@/components/LikeIcon.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const year = new Date().getFullYear() // 页脚版权年份动态生成
const shop = ref({ name: '', score: 0, comments: 0, images: [], address: '', openHours: '' })
const shopBlogs = ref([]) // 该店铺的探店笔记（评论区聚合展示）
const loadError = ref(false) // 店铺加载失败（不存在/已删除）时切换到错误空态
const vouchers = ref([])
const orders = ref([])
const ordersVisible = ref(false)
const shopDetailVisible = ref(false) // 店铺详情弹窗开关
const nowTs = ref(Date.now()) // 当前时间戳，每秒刷新，驱动秒杀倒计时
const seckilling = ref(false) // 秒杀请求在途标记：防快速连点重复下单

let timer = null

// 代金券列表（过滤已结束的秒杀券；isEnd 内部依赖 nowTs，computed 会随倒计时刷新）
const activeVouchers = computed(() => vouchers.value.filter((v) => !isEnd(v)))

onMounted(() => {
  const shopId = route.params.id
  queryShopById(shopId)
  queryVoucher(shopId)
  queryShopBlogs(shopId)
  // 每秒刷新当前时间，驱动倒计时重渲染
  timer = setInterval(() => {
    nowTs.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  clearInterval(timer)
})

function goBack() {
  router.back()
}

function share() {
  // 复制当前店铺链接到剪贴板，便于分享给好友
  const url = location.href
  const notify = () => ElMessage.success('店铺链接已复制，快去分享吧')
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
  // 兼容不支持 Clipboard API 的浏览器
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

function queryShopById(shopId) {
  shopApi
    .detail(shopId)
    .then(({ data }) => {
      // 接口对不存在的 id 返回空对象（无 id 字段），此时不能继续渲染兜底字段，
      // 否则会呈现“暂无店名 + 0 分”的假页面
      if (!data || !data.id) {
        loadError.value = true
        return
      }
      if (data.images) {
        data.images = data.images.split(',')
      } else {
        data.images = []
      }
      data.name = data.name || '暂无店名'
      data.score = data.score || 0
      data.comments = data.comments || 0
      data.address = data.address || ''
      data.openHours = data.openHours || ''
      shop.value = data
    })
    .catch((err) => {
      console.error('加载店铺信息失败:', err)
      loadError.value = true
      ElMessage.error('加载店铺信息失败，请稍后重试')
    })
}

function queryVoucher(shopId) {
  voucherApi
    .listByShop(shopId)
    .then(({ data }) => {
      vouchers.value = data || []
    })
    .catch((err) => {
      console.error('加载代金券失败:', err)
      vouchers.value = []
    })
}

function queryShopBlogs(shopId) {
  // 评论区聚合该店铺的探店笔记；加载失败不阻断主流程，降级为空态
  blogApi
    .ofShop(shopId)
    .then(({ data }) => {
      shopBlogs.value = (data || []).map((b) => ({
        ...b,
        img: (b.images || '').split(',')[0],
      }))
    })
    .catch(() => {
      shopBlogs.value = []
    })
}

// 价格格式化：分 → 元（迁移自旧 common.js util.formatPrice）
function formatPrice(val) {
  if (typeof val === 'string') {
    if (isNaN(val)) return null
    const index = val.lastIndexOf('.')
    let p = ''
    if (index < 0) {
      p = val + '00'
    } else if (index === val.length - 2) {
      p = val.replace('.', '') + '0'
    } else {
      p = val.replace('.', '')
    }
    return parseInt(p)
  } else if (typeof val === 'number') {
    if (!val) return null
    const s = val + ''
    if (s.length === 1) return '0.0' + val
    if (s.length === 2) return '0.' + val
    const i = s.indexOf('.')
    if (i < 0) return s.substring(0, s.length - 2) + '.' + s.substring(s.length - 2)
    const num = s.substring(0, i) + s.substring(i + 1)
    if (i === 1) return '0.0' + num
    if (i === 2) return '0.' + num
    if (i > 2) return num.substring(0, i - 2) + '.' + num.substring(i - 2)
  }
}

function formatTime(t) {
  // 兼容 LocalDateTime 数组 [y,m,d,h,mi,s] 与字符串
  if (!t) return ''
  if (Array.isArray(t)) {
    const p = (n) => (n < 10 ? '0' + n : '' + n)
    return `${t[0]}-${p(t[1])}-${p(t[2])} ${p(t[3] || 0)}:${p(t[4] || 0)}`
  }
  return String(t).replace('T', ' ').split('.')[0].substring(0, 16)
}

function formatMinutes(m) {
  if (m < 10) m = '0' + m
  return m
}

function isNotBegin(v) {
  return new Date(v.beginTime).getTime() > nowTs.value
}

function isEnd(v) {
  return new Date(v.endTime).getTime() < nowTs.value
}

function countdownText(v) {
  // 秒杀倒计时：未开始显示距开始，进行中显示距结束
  const begin = new Date(v.beginTime).getTime()
  const end = new Date(v.endTime).getTime()
  if (nowTs.value < begin) {
    return '距开始 ' + countdownHms(begin - nowTs.value)
  }
  if (nowTs.value < end) {
    return '距结束 ' + countdownHms(end - nowTs.value)
  }
  const b = new Date(v.beginTime)
  const e = new Date(v.endTime)
  return (
    b.getMonth() + 1 + '月' + b.getDate() + '日 ' + b.getHours() + ':' + formatMinutes(b.getMinutes()) +
    ' ~ ' + e.getHours() + ':' + formatMinutes(e.getMinutes())
  )
}

function countdownHms(ms) {
  let s = Math.max(0, Math.floor(ms / 1000))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return pad(h) + ':' + pad(m) + ':' + pad(sec)
}

function showOrders() {
  if (!userStore.isLoggedIn) {
    ElMessage.error('请先登录')
    setTimeout(() => {
      sessionStorage.setItem('login_from', location.pathname + location.search)
      router.push('/login')
    }, 200)
    return
  }
  voucherApi
    .orders()
    .then(({ data }) => {
      orders.value = data || []
      ordersVisible.value = true
    })
    .catch((err) => ElMessage.error(err))
}

function orderStatus(s) {
  return ORDER_STATUS_TEXT[s] || '未知'
}

function payOrder(o) {
  // 模拟支付（演示环境），成功后刷新订单列表展示核销码
  voucherApi
    .pay(o.id)
    .then(() => {
      ElMessage.success('支付成功，到店出示核销码即可使用')
      showOrders()
    })
    .catch((err) => ElMessage.error(err))
}

function seckill(v) {
  // 请求在途时忽略重复点击（防并发重复下单）
  if (seckilling.value) return

  if (!userStore.isLoggedIn) {
    ElMessage.error('请先登录')
    setTimeout(() => {
      sessionStorage.setItem('login_from', location.pathname + location.search)
      router.push('/login')
    }, 200)
    return
  }

  if (isNotBegin(v)) {
    ElMessage.error('优惠券抢购尚未开始！')
    return
  }

  if (isEnd(v)) {
    ElMessage.error('优惠券抢购已经结束！')
    return
  }

  if (v.stock < 1) {
    ElMessage.error('库存不足，请刷新再试试！')
    return
  }

  seckilling.value = true
  voucherApi
    .seckill(v.id)
    .then(({ data }) => {
      ElMessage.success('抢购成功，订单id：' + data)
      // 抢购成功后自动打开订单列表，方便用户查看订单
      showOrders()
    })
    .catch((err) => ElMessage.error(err))
    .finally(() => {
      seckilling.value = false
    })
}
</script>

<style>
/* 头部右侧按钮（覆盖全局 .header-share 的 10% 宽度，对齐旧 shop-detail 布局） */
.shop-detail-page .header-share {
  width: auto;
  min-width: 36px;
  text-align: center;
  font-size: 18px;
  color: #22272b;
  font-weight: bold;
  cursor: pointer;
  flex-shrink: 0;
}
.shop-detail-page .header-back-btn {
  flex-shrink: 0;
}
/* 订单入口按钮（覆盖 .header-share 的 18px，与旧布局一致） */
.shop-detail-page .header-order-btn {
  font-size: 13px;
}
/* 店铺服务标签分隔符/图标 */
.shop-detail-page .shop-service-sep {
  color: #5a5b5b;
}
.shop-detail-page .shop-service-icon {
  margin: 0 5px;
}
/* 评论空态 */
.shop-detail-page .comments-empty {
  text-align: center;
  color: var(--text-muted);
  padding: 20px 0;
  font-size: 14px;
}
/* 评论区探店笔记聚合：横向滚动卡片条 */
.shop-detail-page .comments-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.shop-detail-page .comments-sub {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 400;
}
.shop-detail-page .shop-blog-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 2px 8px;
  scrollbar-width: none;
}
.shop-detail-page .shop-blog-strip::-webkit-scrollbar {
  display: none;
}
.shop-detail-page .shop-blog-card {
  flex-shrink: 0;
  width: 132px;
  border-radius: var(--radius-md);
  border: 1px solid #f0f0f0;
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--dur-base) var(--ease), box-shadow var(--dur-base) var(--ease);
}
.shop-detail-page .shop-blog-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-2);
}
.shop-detail-page .shop-blog-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background: var(--surface-inset);
}
.shop-detail-page .shop-blog-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.shop-detail-page .shop-blog-title {
  padding: 6px 8px 2px;
  font-size: 12px;
  line-height: 1.3;
  color: var(--text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}
.shop-detail-page .shop-blog-foot {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px 8px;
}
.shop-detail-page .shop-blog-avatar {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.shop-detail-page .shop-blog-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--text-muted);
}
.shop-detail-page .shop-blog-liked {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--accent-weak);
}
/* 店铺不存在空态 */
.shop-detail-page .shop-missing {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.shop-detail-page .shop-missing-text {
  font-size: 15px;
  color: var(--text-weak);
}
.shop-detail-page .shop-missing-btn {
  padding: 8px 24px;
  border-radius: var(--radius-pill);
  background: var(--brand);
  color: var(--brand-on);
  box-shadow: var(--shadow-brand);
  font-size: 14px;
  cursor: pointer;
}
/* 店铺详情弹窗正文 */
.shop-detail-page .dialog-detail {
  font-size: 14px;
  line-height: 2;
  color: #333;
}
/* 秒杀订单弹窗 */
.shop-detail-page .orders-empty {
  text-align: center;
  color: var(--text-muted);
  padding: 10px 0;
}
.shop-detail-page .order-item {
  border-bottom: 1px solid #eee;
  padding: 10px 0;
}
.shop-detail-page .order-title {
  font-size: 14px;
  color: #222;
}
.shop-detail-page .order-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
.shop-detail-page .order-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}
.shop-detail-page .order-actions {
  margin-top: 6px;
}
.shop-detail-page .order-code {
  font-size: 12px;
  color: var(--brand);
}
</style>
