// 构建后处理（L2）：
// vite build 将 SPA 产物输出到 html/dist/app（nginx location /app/ 服务）
// 此处将旧 MPA 页面（html/hmdp）全套原样拷贝到 html/dist 根目录，
// 保证旧页面/资源可继续访问，实现新旧双入口并存与回滚兼容。
import { cpSync, existsSync, rmSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const legacySrc = fileURLToPath(new URL('../html/hmdp/', import.meta.url))
const distRoot = fileURLToPath(new URL('../html/dist/', import.meta.url))

// 清空 dist 根下的旧产物（保留 vite 输出的 app/ 子目录）
for (const item of ['index.html', 'assets', 'css', 'js', 'imgs', 'favicon.ico']) {
  const p = distRoot + item
  if (existsSync(p)) {
    rmSync(p, { recursive: true, force: true })
  }
}

// 整目录拷贝旧 MPA 到 dist 根
cpSync(legacySrc, distRoot, { recursive: true })
console.log('[postbuild] copied legacy MPA (html/hmdp) -> dist/')

console.log('[postbuild] done: dist/ = legacy MPA, dist/app/ = Vue3 SPA')
