import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    port: 5173,
    host: '0.0.0.0', // Allow external connections from any IP
    strictPort: true, // Exit if port is already in use
    open: false // Don't auto-open browser (optional for network access)
  },
  build: {
    target: 'es2015',
    outDir: 'dist',
    assetsDir: 'assets'
  },
  optimizeDeps: {
    include: ['pdfjs-dist']
  },
  worker: {
    format: 'es'
  }
})
