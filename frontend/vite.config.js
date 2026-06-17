import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Proxy /api sang backend Spring Boot de tranh cau hinh CORS khi dev.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true, // bind 0.0.0.0 — cho điện thoại/thiết bị khác trong LAN truy cập
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
