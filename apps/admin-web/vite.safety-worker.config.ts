import {defineConfig} from 'vite';
export default defineConfig({build:{ssr:'scripts/safety-worker.ts',outDir:'dist-safety-worker',target:'node24',minify:false,rollupOptions:{output:{entryFileNames:'worker.mjs'}}}});
