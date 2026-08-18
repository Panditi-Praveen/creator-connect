import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import type { IncomingMessage } from 'node:http'

// The backend is only reachable through the API Gateway (port 8080) — the
// single entry point for every client request. The dev server proxies the
// gateway base paths so the browser never hits CORS; a production build
// talks to the gateway directly via VITE_API_BASE_URL (see src/api/client.ts).
const GATEWAY_TARGET = 'http://localhost:8080'

// SPA routes share names with the proxied API prefixes (/projects, /profile,
// /hiring, /ai). A full-page navigation (deep link or refresh) carries an
// Accept: text/html header and MUST be answered with the SPA shell — not
// forwarded to the gateway (which would answer 401/405 JSON and replace the
// app with an error document). API calls (axios, Accept: application/json)
// are proxied as usual.
function isDocumentRequest(req: IncomingMessage): boolean {
  return req.headers.accept?.includes('text/html') ?? false
}

function apiProxy() {
  return {
    target: GATEWAY_TARGET,
    changeOrigin: true,
    bypass: (req: IncomingMessage) => (isDocumentRequest(req) ? '/index.html' : undefined),
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/auth': apiProxy(),
      '/profile': apiProxy(),
      '/projects': apiProxy(),
      '/hiring': apiProxy(),
      '/ai': apiProxy(),
    },
  },
})
