import {defineConfig} from 'vite';
export default defineConfig({ssr:{noExternal:['zod']},build:{copyPublicDir:false,ssr:true,outDir:'dist-safety-worker',target:'node24',minify:false,rollupOptions:{input:{worker:'scripts/safety-worker.ts','check-provider':'scripts/check-safety-provider.ts'},output:{entryFileNames:'[name].mjs'}}}});
