/**
 * 模板中需要以 JS 变量形式传入的颜色（如 SVG fill、Element Plus 的 text-color 属性）。
 *
 * 唯一设计变量源是 styles/tokens.css，此文件仅镜像其中少数需要在 JS 侧使用的值。
 * 修改颜色时必须同时改 tokens.css，两边保持一致；模板内禁止写死颜色字面量。
 *
 * 对应关系：
 *   BRAND_COLOR    -> --brand
 *   TEXT_SECONDARY -> --text-muted（次级/未选中态）
 *   INFO_ICON_COLOR-> --info
 *   RATE_TEXT_COLOR-> --brand
 */
export const BRAND_COLOR = '#ff6633' // 品牌橙：点赞选中/链接/强调按钮
export const TEXT_SECONDARY = '#82848a' // 次级文本灰：点赞未选中/占位文案
export const INFO_ICON_COLOR = '#1890ff' // 信息图标蓝：地址/营业时间等
export const RATE_TEXT_COLOR = '#ff6633' // 评分文字色（与品牌色统一，旧值 #F63 为同色简写）
