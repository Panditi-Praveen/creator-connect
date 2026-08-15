import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend is only reachable through the API Gateway (port 8080) — the
// single entry point for every client request. The dev server proxies the
// gateway base paths so the browser never hits CORS; a production build
// talks to the gateway directly via VITE_API_BASE_URL (see src/api/client.ts).
const GATEWAY_TARGET = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/auth': { target: GATEWAY_TARGET, changeOrigin: true },
      '/profile': { target: GATEWAY_TARGET, changeOrigin: true },
      '/projects': { target: GATEWAY_TARGET, changeOrigin: true },
      '/hiring': { target: GATEWAY_TARGET, changeOrigin: true },
      '/ai': { target: GATEWAY_TARGET, changeOrigin: true },
    },
  },
})
