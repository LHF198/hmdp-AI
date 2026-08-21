# hmdp — Agent 导航文件

> 黑马点评：餐饮商铺评价平台。Spring Boot 3.5.16 + Vue 3.5.41 SPA 全栈。

## 1. 项目概述

本地生活服务点评平台（商铺评价 + 笔记分享 + 优惠券秒杀 + AI 探店助手）。

- **后端**：`src/main/java/com/hmdp/` — Spring Boot 3.5.16 + MyBatis-Plus 3.5.17 + Redis 7.2.15 + Spring AI 1.1.8
- **前端**：`frontend/src/` — Vue 3.5.41 + Vite 7.3.6 + Element Plus 2.14.4 + Pinia 3.0.4 + Vue Router 5.2.0
- **测试**：`src/test/java/com/hmdp/`
- **配置**：`src/main/resources/`

## 2. 快速命令

### 启动（必须按序）

```powershell
# 1. Redis 7.2.15
redis-server

# 2. MySQL 8.0（确认 hmdp 库已创建，已导入 src/main/resources/db/hmdp.sql）

# 3. 后端（端口 8081）
pwsh scripts/start_backend.ps1        # 或：mvn spring-boot:run

# 4. 前端 nginx（端口 8080）
cd frontend && .\nginx.exe
```

### 构建与测试

| 操作 | 命令 |
|---|---|
| 后端编译 | `mvn compile` |
| 后端测试 | `mvn test` |
| 后端打包 | `mvn package -DskipTests` |
| 前端开发 | `cd frontend && npm run dev` |
| 前端构建 | `cd frontend && npm run build` |
| 环境初始化 | `pwsh scripts/setup_env.ps1` |

### 环境变量

| 变量 | 用途 | 默认值 |
|---|---|---|
| `AI_API_KEY` | AI 模型密钥（阿里云百炼 DashScope） | 空（降级启动，禁用 AI） |
| `HMDP_DB_PASSWORD` | MySQL 密码 | `1234` |

## 3. 后端架构

> 编码规范详见 `.qoder/rules/java-backend.md`，核心子系统实现详见 `.qoder/docs/backend-architecture.md`

| 子系统 | 关键实现 |
|---|---|
| 缓存 | `CacheClient` 封装穿透/击穿/逻辑过期三种模式 |
| 秒杀 | `seckill.lua` 原子操作 + Redis 预扣库存 + Stream 异步下单 |
| 分布式锁 | Redisson 可重入锁 + `unlock.lua` 原子释放 |
| AI 助手 | `com.hmdp.ai` 独立分层，Spring AI + RAG + Redis 会话记忆 |

代码示例——CacheClient 三种模式：
```java
// 穿透
cacheClient.queryWithPassThrough("cache:shop:" + id, Shop.class, () -> getById(id), 30L, MINUTES);
// 击穿
cacheClient.queryWithMutex("cache:shop:" + id, Shop.class, () -> getById(id), 30L, MINUTES);
// 逻辑过期
cacheClient.queryWithLogicalExpiration("cache:shop:" + id, Shop.class, () -> getById(id), 30L, MINUTES);
```

## 4. 前端架构

> 编码规范详见 `.qoder/rules/frontend-vue.md`，CSS 架构详见 `.qoder/docs/frontend-css-architecture.md`

- **路由**：`frontend/src/router/index.js`，懒加载，401 驱动登录（无 `beforeEach` 守卫）
- **API 层**：`frontend/src/api/` 按业务域拆分（`http.js` 统一封装 + `blog.js` / `shop.js` / `user.js` / `follow.js` / `message.js` / `voucher.js`）
- **CSS 分层**：`tokens.css` → `base.css` → `layout.css` → `components.css` → `page-*.css`
- **构建部署**：Vite SPA 单入口（`index.html`），nginx 分流静态/动态请求

## 5. 关键约定

### ✅ 必须做

- 用 `@Resource`（Jakarta）注入依赖
- Controller 返回 `Result`（`Result.ok()` / `Result.fail()`），前端 `http.js` 自动解包
- 前端组件用 `<script setup>` 语法
- CSS 颜色/圆角/阴影引用 `tokens.css` 变量；JS 侧从 `utils/colors.js` 取
- 页面级样式收窄到页面根类（如 `.info-page`、`.login-container`）
- 悬浮元素用 `<Teleport to="body">`（参考 FootBar.vue / AiLauncher.vue）
- 未登录场景的组件发请求前先检查 `sessionStorage` token

### ⚠️ 先确认再改

- 修改 `application.yaml` 中的 AI 配置：确认 `base-url` 不含 `/v1` 后缀
- 修改 CSS 层叠顺序：确认不会引入新的优先级冲突
- 新增 Redis key：确认命名遵循 `业务:子业务:id` 格式

### 🚫 绝对禁止

- `@Autowired`（用 `@Resource`）
- 在 `#app` 内新增 `position: fixed` 元素（`backdrop-filter` 包含块陷阱）
- 在模板或 CSS 中写死颜色/圆角/阴影字面量（必须引用 `tokens.css` 变量）
- 用 `!important` 抢优先级（叠层已消除，靠选择器作用域收窄）
- 手动 `import { ElButton } from 'element-plus'`（按需自动导入）
- 在 Controller 中直接返回 Map/String（必须包装为 `Result`）

## 6. 本地开发及验证流程

### 闭环：改 → 构建 → 启动 → 验证

1. **改代码**：编辑 Java 或 Vue 源文件
2. **构建**：后端 `mvn compile` / 前端 `cd frontend && npm run build`（开发用 `npm run dev`，HMR 自动刷新）
3. **启动**：Redis → MySQL → 后端（8081）→ nginx（8080）
4. **验证**：
   - 浏览器 `http://localhost:8080`（SPA 入口）
   - 后端直测 `http://localhost:8081/shop/1`
   - 日志：`logs/backend.log`（运行日志）、`logs/backend_err.log`（错误日志）

### Harness 自动验证

编辑后自动触发（`.qoder/hooks/post-edit-validation.json`）：
- Java 修改 → `mvn test`（120s）
- Vue/JS/CSS 修改 → `vite build`（60s）
- YAML 修改 → `mvn compile`（120s）

## 7. 环境陷阱（踩坑记录）

> 完整详解见 `.qoder/docs/env-pitfalls.md`，以下为高频陷阱摘要：

| 陷阱 | 要点 |
|---|---|
| PowerShell mysql | 必须长参数 `mysql --user=root --password=1234`，短参数 `-u` 被路径转换 |
| redis-cli 通配符 | 先设 `$env:MSYS_NO_PATHCONV = 1`，否则 `*` 被转为路径 |
| 中文路径 .ps1 | 必须用 `pwsh`，不能用 `powershell.exe` |
| Spring AI base-url | 不能含 `/v1` 后缀，否则 404 |
| Lombok + Maven | `pom.xml` 需显式配 `annotationProcessorPaths` |
| `#app` + fixed | `backdrop-filter` 创建包含块，fixed 被劫持 → 用 Teleport 到 body |
| EP 按需样式覆盖 | 用 `body .el-button` 高特异性选择器，不能依赖加载顺序 |
| 未登录发鉴权请求 | 401 拦截会全局跳登录 → 先检查 sessionStorage token |
| 瀑布流图片比例 | 测容器 `offsetWidth` 设精确高度，移除 `.blog-list` 固定高度 |

## 8. Harness 资产索引

| 类型 | 路径 | 说明 |
|---|---|---|
| Rule | `.qoder/rules/java-backend.md` | Java 后端编码约定 |
| Rule | `.qoder/rules/frontend-vue.md` | 前端 Vue 编码约定 |
| Hook | `.qoder/hooks/post-edit-validation.json` | 编辑后自动验证 |
| Doc | `.qoder/docs/env-pitfalls.md` | 环境陷阱详解 |
| Doc | `.qoder/docs/backend-architecture.md` | 后端核心子系统实现 |
| Doc | `.qoder/docs/frontend-css-architecture.md` | 前端 CSS 架构详情 |

## 9. 文档导航

| 文档 | 路径 |
|---|---|
| 中文 README | `README.md` |
| English README | `README.en.md` |
| 应用主配置 | `src/main/resources/application.yaml` |
| 开发环境配置 | `src/main/resources/application-dev.yaml` |
| 生产环境配置 | `src/main/resources/application-prod.yaml` |
| 数据库脚本 | `src/main/resources/db/hmdp.sql` |
| 数据库优化迁移 | `src/main/resources/db/migration_v2_db_optimize.sql` |
| RAG 知识库 | `src/main/resources/knowledge/店铺信息.md` |
| AI 系统提示词 | `src/main/resources/prompts/system-prompt.st` |
| 秒杀 Lua | `src/main/resources/seckill.lua` |
| 分布式锁 Lua | `src/main/resources/unlock.lua` |
| MyBatis XML | `src/main/resources/mapper/VoucherMapper.xml` |
| nginx 配置 | `frontend/conf/nginx.conf` |
| 前端入口 | `frontend/index.html` |
| Vite 配置 | `frontend/vite.config.js` |
| 前端依赖 | `frontend/package.json` |
| 后端依赖 | `pom.xml` |
