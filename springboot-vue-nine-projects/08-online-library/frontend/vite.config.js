import { defineConfig } from 'vite'; import vue from '@vitejs/plugin-vue';
export default defineConfig({ plugins:[vue()], server:{port:5108, proxy:{'/api':{target:'http://localhost:8108',changeOrigin:true}}} });
