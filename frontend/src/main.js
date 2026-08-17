import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 按需引入模板实际用到的图标（全量注册会把 ~300 个图标全部打进首屏包，改按需后产物显著减小）
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Box,
  Camera,
  CaretBottom,
  CaretTop,
  ChatDotRound,
  Close,
  Delete,
  Edit,
  HomeFilled,
  InfoFilled,
  Loading,
  Location,
  MapLocation,
  Plus,
  Promotion,
  Pointer,
  Search,
  Service,
  SwitchButton,
  User,
  WarningFilled,
  Watch,
} from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import FootBar from '@/components/layout/FootBar.vue'
import { setRouter } from './api/http'
import { imgFade } from './utils/img-fade'

// 全局样式：SPA 自有样式体系（旧 MPA 的 html/hmdp/css 已全部退役）
// 加载顺序即层叠顺序，由通用到具体，不再依赖 !important 抢优先级：
//   tokens（唯一设计变量源）→ base（reset/排版）→ layout（外壳/页头/底栏）
//   → components（按钮/输入/卡片/空态）→ 各页面样式（按页作用域收窄）
import './styles/tokens.css'
import './styles/base.css'
import './styles/layout.css'
import './styles/components.css'
// 页面级样式（类名作用域已收窄到各自页面根类，不会互相泄漏）
import './styles/page-home.css' // 首页（分类宫格/笔记瀑布流/城市选择）
import './styles/page-shop-list.css' // 店铺列表（排序栏/店铺卡片）
import './styles/page-info.css' // 个人主页/他人主页（.info-page）
import './styles/page-login.css' // 登录（.login-container）
import './styles/blog-edit-spa.css' // 发笔记
import './styles/shop-detail-spa.css' // 店铺详情
import './styles/blog-detail-spa.css' // 笔记详情
import './styles/ai-assistant-spa.css' // AI 探店助手（.ai-page-root）

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 仅注册模板中实际使用的图标（静态白名单，与全量注册语义一致：模板内 <IconName /> 可直接使用）
const icons = {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Box,
  Camera,
  CaretBottom,
  CaretTop,
  ChatDotRound,
  Close,
  Delete,
  Edit,
  HomeFilled,
  InfoFilled,
  Loading,
  Location,
  MapLocation,
  Plus,
  Promotion,
  Pointer,
  Search,
  Service,
  SwitchButton,
  User,
  WarningFilled,
  Watch,
}
for (const [name, component] of Object.entries(icons)) {
  app.component(name, component)
}

// 底部导航全局组件（对齐旧 footer.js 的 footBar 语义，各页面直接 <FootBar /> 使用）
app.component('FootBar', FootBar)

// 图片渐显指令：列表图片加载完成后淡入，减少滚动时的突兀感
app.directive('img-fade', imgFade)

// 注入 router 供 http 层 401 时跳转登录（避免循环依赖）
setRouter(router)

app.mount('#app')
