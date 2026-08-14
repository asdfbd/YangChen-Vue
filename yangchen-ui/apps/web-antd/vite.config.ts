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
          // knife4j 文档：doc.html 经 /api 代理加载后，springdoc 返回的文档地址是绝对路径
          // /v3/api-docs/**（如 /v3/api-docs/default），不走 /api 前缀，需要独立代理，
          // 否则会被 Vite 当作 SPA 路由返回 index.html，导致 knife4j 解析 JSON 失败。
          '/v3': {
            changeOrigin: true,
            target: 'http://localhost:8080',
          },
        },
      },
    },
  };
});
