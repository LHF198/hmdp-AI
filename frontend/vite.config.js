import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'

// L2 策略：Vue 3 SPA 渐进式重构。
// - SPA 入口为 frontend/index.html，产物输出到 html/dist/app（nginx 通过 /app/ 分流）
// - 旧 MPA 页面（html/hmdp）不再作为构建入口，由 scripts/postbuild.mjs 原样拷贝到 dist 根，保证回滚兼容
// - dev 模式 base=/ 直接访问；build 产物 base=/app/ 与 nginx location /app/ 对应
export default defineConfig(({ command }) => ({
  base: command === 'build' ? '/app/' : '/',
  plugins: [
    vue(),
    // Element Plus 按需自动导入（组件 + API，含样式）
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 自定义中间件：在 dev 模式下服务 html/hmdp/imgs 等静态资源
    configureServer(server) {
      const hmdpStaticDir = fileURLToPath(new URL('./html/hmdp', import.meta.url))
      server.middlewares.use((req, res, next) => {
        // 拦截 /imgs/ 开头的请求，从 html/hmdp/imgs 目录提供文件
        if (req.url && req.url.startsWith('/imgs/')) {
          // 去掉查询字符串（如 ?v=123）
          const urlPath = req.url.split('?')[0]
          const filePath = path.join(hmdpStaticDir, urlPath)
          
          if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
            const ext = path.extname(filePath).toLowerCase()
            const mimeTypes = {
              '.jpg': 'image/jpeg',
              '.jpeg': 'image/jpeg',
              '.png': 'image/png',
              '.gif': 'image/gif',
              '.svg': 'image/svg+xml',
              '.webp': 'image/webp',
              '.ico': 'image/x-icon',
            }
            res.setHeader('Content-Type', mimeTypes[ext] || 'application/octet-stream')
            res.setHeader('Cache-Control', 'public, max-age=3600')
            fs.createReadStream(filePath).pipe(res)
            return
          } else {
            console.warn(`[Vite Middleware] Image not found: ${filePath}`)
          }
        }
        next()
      })
    },
    proxy: {
      // AI 接口：后端 ChatController 映射 /api/ai/*，保留前缀不剥离（与 nginx location /api/ai/ 一致）
      '/api/ai': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
      // 业务接口：与 nginx 保持一致，去掉 /api 前缀
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ''),
      },
    },
  },
  build: {
    outDir: fileURLToPath(new URL('./html/dist/app', import.meta.url)),
    emptyOutDir: true,
  },
}))
