# 环境陷阱详解（踩坑记录）

> 本文件从 AGENTS.md 拆分，包含所有环境相关的踩坑记录。每条记录含现象、根因和解决方案。

## PowerShell 中 mysql 参数

- **现象**：`mysql -u root -p1234` 在 PowerShell 中报错或行为异常
- **根因**：msys2 环境下短参数 `-u` 会被路径转换
- **解决**：必须使用长参数格式 `mysql --user=root --password=1234`，并指定 `--default-character-set=utf8mb4` 避免中文乱码

## redis-cli 通配符

- **现象**：`redis-cli KEYS *` 中的 `*` 被转为文件路径
- **根因**：msys2 版 redis-cli 的路径转换机制
- **解决**：执行前设置 `$env:MSYS_NO_PATHCONV = 1`

## 含中文路径的 .ps1 脚本

- **现象**：含中文路径的 `.ps1` 脚本执行失败
- **根因**：`powershell.exe`（Windows PowerShell 5.x）对中文路径编码处理有缺陷
- **解决**：必须用 `pwsh`（PowerShell 7+）执行，不能用 `powershell.exe`

## Spring AI base-url 配置

- **现象**：AI 接口返回 404
- **根因**：阿里云百炼 DashScope 兼容 OpenAI 协议，但 base-url **不能包含 `/v1` 后缀**
- **解决**：配置 `spring.ai.openai.base-url` 时去掉末尾的 `/v1`

## Lombok + Maven 编译

- **现象**：编译期注解处理失效，Entity 的 getter/setter 未生成
- **根因**：Maven 编译插件需要显式声明 annotation processor
- **解决**：在 `pom.xml` 的 `maven-compiler-plugin` 中显式配置 `annotationProcessorPaths` 包含 Lombok

## backdrop-filter + fixed 定位（CSS）

- **现象**：`position: fixed` 元素在内容超高时被推到页面底部，而非固定在视口
- **根因**：CSS `backdrop-filter` 会创建新的包含块（containing block），导致 `#app` 内部 fixed 元素的定位参照变成 `#app` 而非视口。`layout.css` 的 `#app` 带 `backdrop-filter`
- **解决**：悬浮元素一律用 Vue `<Teleport to="body">` 绕开此坑。已实现的有 `.foot`（FootBar.vue）和 `.ai-launcher`（AiLauncher.vue），`BlogEdit` 头部同样使用 Teleport
- 🚫 **禁止在 `#app` 内新增 `position: fixed` 元素**

## Element Plus 按需样式覆盖

- **现象**：对 EP 组件的全局样式覆盖（如按钮颜色）在某些页面失效
- **根因**：EP 组件样式按路由懒加载成独立 chunk（如 `el-button-*.css`），在 `index-*.css` **之后**追加，会覆盖全局样式
- **解决**：对 EP 组件的全局覆盖必须用 `body .el-button` 这类**高特异性选择器**（在 `components.css` 中已统一处理），不能依赖加载顺序

## 未登录场景勿发需鉴权请求

- **现象**：未登录页面被劫持跳转到登录页
- **根因**：`http.js` 的 401 拦截器会全局跳转登录页并记录来源页。任何在未登录场景挂载的组件（如 FootBar 查未读消息）如果直接发鉴权请求，401 响应会触发跳转
- **解决**：未登录场景的组件**必须先检查 `sessionStorage` 中的 token 再发请求**（历史 bug，已修复）

## 前端构建需完整子进程权限

- **现象**：`vite build` 报 `spawn EPERM` 错误
- **根因**：`vite build` 依赖 esbuild spawn 子进程（piped stdio），在受限沙箱下权限不足
- **解决**：确保在完整权限的终端中执行，这不是代码错误

## 瀑布流卡片图片比例自适应

- **现象**：首页瀑布流卡片图片有大片留白，或图片下半部分被裁掉
- **根因**：三个连环坑——
  1. `object-fit: contain` 在容器比例与图片不完全匹配时产生 letterboxing 留白
  2. 用 `43vw` 近似计算列宽有偏差，导致容器比例与图片不一致
  3. `.blog-box` 有 `overflow: hidden` + `.blog-list` 有 `height: 60vh` → 卡片高度被限制
- **解决**：
  - 让容器比例与图片**完全一致**：`onCardImgLoad` 中测量容器实际 `offsetWidth`，按 `height = width / ratio` 设置精确 inline height
  - **移除 `.blog-list` 的固定高度**，改为 `overflow: visible` + `grid-auto-rows: minmax(0, auto)`，让网格随内容自然增长，页面整体滚动
  - `queryHotBlogsScroll` 中 `nextTick` 对缓存比例的卡片直接设高度（`@load` 可能因浏览器缓存不触发）
- 代码要点：
  ```js
  function onCardImgLoad(b, e) {
    const img = e.target
    if (img.naturalWidth && img.naturalHeight) {
      const ratio = img.naturalWidth / img.naturalHeight
      const box = imgBoxRefs.get(b.id)
      if (box) {
        const w = box.offsetWidth
        if (w > 0) {
          box.style.height = Math.round(w / ratio) + 'px'
          box.style.aspectRatio = ''
        }
      }
    }
  }
  ```
