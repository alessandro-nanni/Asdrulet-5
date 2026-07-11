import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
      // Spring Security's OAuth2 login endpoints, so the login button can use
      // a plain relative URL in both dev (via this proxy) and prod (same origin).
      '/oauth2': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
    },
  },
})
