---
description: 前端 Vue 3 SPA 编码约定与组件规范，修改 frontend/src 下任何文件时自动加载
globs: frontend/src/**/*.{vue,js,css}
alwaysApply: false
---

# 前端 SPA 模块约定

## 技术栈

- Vue 3.5.41 + Vite 7.3.6 + Composition API（`<script setup>` 语法）
- Vue Router 5.2.0（`createWebHistory`，懒加载）
- Pinia 3.0.4（`defineStore` 选项式风格）
- Element Plus 2.14.4（按需自动导入，**不手动 import 组件**）
- Axios 1.19.0（封装在 `api/http.js`，baseURL `/api`）
- 路径别名：`@` → `src/`

## 目录结构与职责

| 目录 | 职责 | 约定 |
|---|---|---|
| `api/` | HTTP 请求封装 | 按业务域拆分文件，导出 `xxxApi` 对象 |
| `views/` | 页面级 Vue 组件 | 一个路由对应一个 `.vue` 文件 |
| `components/` | 公共/跨页面组件 | 仅放真正复用的组件（如 `FootBar`、`AiLauncher`） |
| `router/` | 路由配置 | `router/index.js`，统一出口 |
| `stores/` | Pinia 状态管理 | `useXxxStore` 命名 |
| `styles/` | 页面级 CSS | 按页面拆分，全局样式放 `main.js` 引入 |
| `utils/` | 工具函数 | 纯函数，无副作用 |

## 强制约定

### Vue 组件
- **必须**使用 `<script setup>` 语法（Composition API）
- 响应式状态用 `ref()`，复杂对象用 `reactive()`
- 生命周期钩子：`onMounted`、`onBeforeUnmount` 等
- 组件文件命名：PascalCase（如 `ShopDetail.vue`）
- 页面组件顶部注释说明用途（一行即可）

### Element Plus 使用
- **按需自动导入**：通过 `unplugin-auto-import` + `unplugin-vue-components` 配置
- **不要**手动 `import { ElButton } from 'element-plus'`
- **可以**手动导入 `ElMessage` / `ElMessageBox`（函数式调用不在组件注册范围）
- 图标使用 `@element-plus/icons-vue`，按需导入具体图标组件

### API 层规范
```javascript
import http from './http'

export const shopApi = {
  detail: (id) => http.get('/shop/' + id),
  // ...
}
```
- 每个业务域一个文件，导出 `xxxApi` 对象
- 方法只做请求转发，不处理业务逻辑
- `http.js` 已自动解包 `Result`（`response.data`），401 自动跳登录
- 请求错误由 `http.js` 统一拦截，调用方 `.catch()` 只需展示错误信息

### 路由规范
- 使用 `createWebHistory` 模式
- 页面组件懒加载：`component: () => import('@/views/Xxx.vue')`
- 每个路由设置 `meta.title` 用于页面标题

### 状态管理
- 使用 Pinia `defineStore`
- Store 文件命名：`useXxxStore` 风格（如 `user.js`、`city.js`）
- 持久化到 `sessionStorage` / `localStorage` 由 Store 自行管理

### CSS 约定

样式全部由 `src/styles/` 拥有（旧 MPA 的 `html/hmdp/css/` 已退役删除）。层叠顺序即 `main.js` 引入顺序，由通用到具体：

| 文件 | 职责 |
|---|---|
| `tokens.css` | **唯一设计变量源**：颜色/圆角/阴影/模糊/字体/间距/层级 |
| `base.css` | reset、根字号、页面背景光斑、排版基线 |
| `layout.css` | `#app` 外壳、`.header`、`.foot`、`.search-bar` |
| `components.css` | 按钮体系、输入框、`.glass-card`、`.empty-state`、Element Plus 适配 |
| `page-*.css` | 页面级样式，必须收窄到页面根类 |

强制规则：

- **禁止写死颜色/圆角/阴影字面量**，一律引用 `tokens.css` 变量；模板中需 JS 传色的场景（SVG `fill`、`el-rate` 的 `text-color`）从 `utils/colors.js` 引用
- **禁止用 `!important` 抢优先级**。旧的「旧样式打底 + 新样式 `!important` 覆盖」叠层已彻底消除，冲突一律靠选择器作用域收窄解决
- **对 EP 组件的全局覆盖必须用高特异性选择器**（如 `body .el-button`）：EP 按需样式按路由懒加载成独立 chunk，在 `index-*.css` 之后追加，低特异性规则会被覆盖
- **页面级样式必须收窄到页面根类**（如 `.info-page`、`.login-container`、`.shop-detail-page`），禁止裸写 `.header` / `.blog-title` 等共享类名，否则会泄漏到其他页面
- **不要在 `#app` 内新增 `position: fixed` 元素**：`#app` 的 `backdrop-filter` 创建包含块，fixed 定位会被劫持为相对 `#app`（内容超高时被推到页面底部）。悬浮元素一律 Teleport 到 `body`（参考 FootBar.vue / AiLauncher.vue）
- 组件内 `<style>` 优先加 `scoped`，且只放该页面独有的样式
- 按钮层级：主操作用 `<el-button type="primary">`（品牌橙），次级用默认 `<el-button>`（玻璃白），危险用 `type="danger"`；不要自己覆盖按钮底色
- 空态用 `<EmptyState text="..." />`，点赞图标用 `<LikeIcon :active="..." />`，卡片用 `.glass-card`，勿重复实现
- 图片渐显用 `v-img-fade` 指令

### 构建与部署
- 开发：`npm run dev`（Vite dev server，`/api` 代理到 `http://127.0.0.1:8081`）
- 构建：`npm run build`（产物输出到 `html/dist/app/`，base 路径 `/app/`）
- 构建后由 nginx 分发，静态资源走 8080 端口
