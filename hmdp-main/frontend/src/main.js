import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import FootBar from '@/components/layout/FootBar.vue'
import { setRouter } from './api/http'

// 全局样式：直接复用旧 MPA 手写样式，保证玻璃拟态主题与旧页面一致
// 加载顺序与旧页面一致：各页面专属 CSS 在前，glass.css 覆盖层最后生效
import '../html/hmdp/css/main.css'
import '../html/hmdp/css/index.css' // 首页（类型列表/笔记瀑布流）
import '../html/hmdp/css/shop-list.css' // 店铺列表
import './styles/blog-edit-spa.css' // 发笔记（blog-edit.css 的 SPA 改写版，隔离全局冲突）
import '../html/hmdp/css/info.css' // 资料/他人主页
import './styles/shop-detail-spa.css' // 店铺详情（shop-detail.css 的 SPA 改写版）
import './styles/blog-detail-spa.css' // 笔记详情（blog-detail.css 的 SPA 改写版）
import '../html/hmdp/css/glass.css' // 玻璃拟态覆盖层
import '../html/hmdp/css/login.css'
// Element Plus 结构兼容层（玻璃拟态输入框样式适配）
import './styles/ep-override.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 全局注册 Element Plus 图标组件（模板中直接 <el-icon><ChatDotRound /></el-icon> 使用）
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

// 底部导航全局组件（对齐旧 footer.js 的 footBar 语义，各页面直接 <FootBar /> 使用）
app.component('FootBar', FootBar)

// 注入 router 供 http 层 401 时跳转登录（避免循环依赖）
setRouter(router)

app.mount('#app')
