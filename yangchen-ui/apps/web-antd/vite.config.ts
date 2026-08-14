import {defineConfig} from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            // 真实 RuoYi 后端
            target: 'http://localhost:8080',
            ws: true,
          },
        },
      },
    },
  };
});
