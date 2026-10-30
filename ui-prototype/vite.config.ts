import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The SPA must share an origin with the gateway (session + XSRF cookies). In development Vite plays Caddy's role:
// port 9000 is already a Keycloak redirect URI, and the Host header is kept so the gateway builds its OAuth2
// redirect back to this server rather than to itself.
const gateway = process.env.GATEWAY_URL ?? 'http://localhost:8080'
const proxied = ['/api', '/services', '/oauth2', '/login', '/management']

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 9000,
    strictPort: true,
    proxy: Object.fromEntries(proxied.map(path => [path, { target: gateway, changeOrigin: false }])),
  },
})
