// 构建后处理（L2）：
// vite build 将 SPA 产物输出到 html/dist/app（nginx location /app/ 服务）
// 此处将 html/hmdp 中的静态资源（CSS + 图片）拷贝到 html/dist 根目录，供 nginx 服务
import { cpSync, existsSync, rmSync, readdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const legacySrc = fileURLToPath(new URL('../html/hmdp/', import.meta.url))
const distRoot = fileURLToPath(new URL('../html/dist/', import.meta.url))

// 清空 dist 根下的旧产物（保留 vite 输出的 app/ 子目录）
// 清理所有 HTML 文件（旧 MPA 页面已全部删除，dist 中不应残留任何 .html）
for (const item of ['assets', 'css', 'js', 'imgs', 'favicon.ico']) {
  const p = distRoot + item
  if (existsSync(p)) {
    rmSync(p, { recursive: true, force: true })
  }
}
// 清理 dist 根目录下所有 .html 文件（包括旧 MPA 页面残留）
for (const f of readdirSync(distRoot)) {
  if (f.endsWith('.html')) {
    rmSync(distRoot + f, { force: true })
  }
}

// 拷贝 html/hmdp 中的静态资源到 dist 根
cpSync(legacySrc, distRoot, { recursive: true })
console.log('[postbuild] copied static assets (html/hmdp) -> dist/')

console.log('[postbuild] done: dist/app/ = Vue3 SPA, dist/css+imgs = static assets')
