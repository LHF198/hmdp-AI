import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath } from 'node:url'

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
    proxy: {
      // 与 nginx 保持一致的 /api 前缀转发，dev 时去掉前缀
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
