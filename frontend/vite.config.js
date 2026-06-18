import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 8099,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/mock-images': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/tile-proxy': {
        target: 'https://ditu.zjzwfw.gov.cn',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/tile-proxy/, ''),
        headers: {
          Referer: 'http://127.0.0.1:8099/src/testMap2.html'
        }
      }
    }
  }
})
