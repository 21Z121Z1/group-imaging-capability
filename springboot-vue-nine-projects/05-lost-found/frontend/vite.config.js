import { defineConfig } from 'vite'; import vue from '@vitejs/plugin-vue';
export default defineConfig({ plugins:[vue()], server:{port:5105, proxy:{'/api':{target:'http://localhost:8105',changeOrigin:true}}} });
