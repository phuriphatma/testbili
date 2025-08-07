import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    port: 3000,
    host: '0.0.0.0', // Allow external connections
    open: true
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
