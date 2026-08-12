import { createRouter, createWebHistory } from 'vue-router'

// 路由表：与旧 MPA 页面一一对应（后续阶段逐页替换 Placeholder）
// 登录策略与旧行为等价：不做强制拦截，未登录访问由 http 层 401 驱动跳登录（记录来源页回跳）
const routes = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { title: '手机号码快捷登录' } },
  { path: '/login2', name: 'login2', component: () => import('@/views/Login2.vue'), meta: { title: '密码登录' } },
  { path: '/message', name: 'message', component: () => import('@/views/Message.vue'), meta: { title: '消息中心' } },
  { path: '/shops', name: 'shops', component: () => import('@/views/ShopList.vue'), meta: { title: '店铺列表' } },
  { path: '/shop/:id', name: 'shop-detail', component: () => import('@/views/ShopDetail.vue'), meta: { title: '店铺详情' } },
  { path: '/blog/:id', name: 'blog-detail', component: () => import('@/views/BlogDetail.vue'), meta: { title: '笔记详情' } },
  { path: '/blog/edit', name: 'blog-edit', component: () => import('@/views/BlogEdit.vue'), meta: { title: '发布笔记' } },
  { path: '/profile', name: 'info', component: () => import('@/views/Profile.vue'), meta: { title: '我的' } },
  { path: '/profile/edit', name: 'info-edit', component: () => import('@/views/InfoEdit.vue'), meta: { title: '编辑资料' } },
  { path: '/user/:id', name: 'other-info', component: () => import('@/views/OtherInfo.vue'), meta: { title: '个人主页' } },
  { path: '/map', name: 'map', component: () => import('@/views/Map.vue'), meta: { title: '地图' } },
  { path: '/ai', name: 'aiassistant', component: () => import('@/views/AiAssistant.vue'), meta: { title: 'AI 探店助手' } },
  // 兜底：未知路径回首页
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  // 路由切换滚动回顶：对齐旧 MPA 整页刷新的滚动位置行为
  scrollBehavior() {
    return { top: 0 }
  },
})

// 页面标题守卫
router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '黑马点评'
})

export default router
