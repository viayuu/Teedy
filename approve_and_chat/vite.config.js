import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: true,     // 监听所有可用的网络接口
    port: 3000,     // 使用更常见的端口
    open: true,     // 自动打开浏览器
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/docs-web/api') // 将前端的 /api 替换为后端的 /docs-web/api
      },
      '/data-api': {
        target: 'http://localhost:3001',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/data-api/, '/api') // 将前端的 /data-api 替换为数据服务器的 /api
      }
    }
  }
}) 