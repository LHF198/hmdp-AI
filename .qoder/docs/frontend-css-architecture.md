# 前端 CSS 架构详情

> 本文件从 AGENTS.md 拆分。CSS 编码约定详见 `.qoder/rules/frontend-vue.md`。

## 样式分层体系

全部样式由 `frontend/src/styles/` 拥有，旧 MPA 的 `html/hmdp/css/` 已退役删除。层叠顺序即 `main.js` 引入顺序，由通用到具体：

| 顺序 | 文件 | 职责 |
|---|---|---|
| 1 | `tokens.css` | **唯一设计变量源**：颜色/圆角/阴影/模糊/字体/间距/层级 |
| 2 | `base.css` | reset、根字号、页面背景光斑、排版基线 |
| 3 | `layout.css` | `#app` 外壳、`.header`、`.foot`、`.search-bar` |
| 4 | `components.css` | 按钮体系、输入框、`.glass-card`、`.empty-state`、Element Plus 全局适配 |
| 5 | `page-*.css` | 页面级样式，必须收窄到页面根类 |

## 设计变量引用

- CSS 中：引用 `tokens.css` 变量（如 `var(--color-primary)`）
- JS 中需传色的场景（SVG `fill`、`el-rate` 的 `text-color`）：从 `utils/colors.js` 引用（其值镜像 tokens）

## 通用组件与工具类

| 组件/类 | 用途 |
|---|---|
| `<EmptyState text="..." />` | 空态展示 |
| `<LikeIcon :active="..." />` | 点赞图标 |
| `.glass-card` | 玻璃拟态卡片 |
| `.user-row` | 用户信息行 |
| `.empty-state` | 空态工具类 |
| `v-img-fade` | 图片渐显指令 |

## 按钮层级约定

| 场景 | 组件 |
|---|---|
| 主操作 | `<el-button type="primary">`（品牌橙） |
| 次级操作 | 默认 `<el-button>`（玻璃白） |
| 危险操作 | `<el-button type="danger">` |

不要自己覆盖按钮底色。
