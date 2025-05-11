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
        // 提供两种可能的API路径格式
        rewrite: (path) => {
          // 检查环境变量来决定使用哪种路径
          const useDirectPath = process.env.DIRECT_API === 'true';
          
          if (useDirectPath) {
            // 直接使用/api路径
            return path;
          } else {
            // 使用/docs-web/api路径
            return path.replace(/^\/api/, '/docs-web/api');
          }
        }
      },
      '/docs-web/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/data-api': {
        target: 'http://localhost:3001',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/data-api/, '/api') // 将前端的 /data-api 替换为数据服务器的 /api
      },
      '/chat-api': {
        target: 'http://localhost:3002',  // 聊天服务器地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/chat-api/, '') // 将前端的 /chat-api 替换为聊天服务器的根路径
      }
    }
  }
}) 