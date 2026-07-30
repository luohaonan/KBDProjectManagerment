import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendHost = env.VITE_BACKEND_HOST || '127.0.0.1'
  const backendPort = env.VITE_BACKEND_PORT || '8080'
  const backendProtocol = env.VITE_BACKEND_PROTOCOL || 'http'
  const backendTarget = `${backendProtocol}://${backendHost}:${backendPort}`

  return {
    plugins: [react()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      // 局域网其它设备通过本机 IP 访问时，/api 由开发机转发到开发机本地后端，避免浏览器请求各设备自己的 localhost
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
        },
      },
    },
  }
})