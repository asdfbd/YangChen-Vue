import {useAppConfig} from '@vben/hooks';

/**
 * 富文本 HTML 归一化。
 *
 * 旧项目（Vue2 / Element UI）的公告内容里图片/链接存的是旧 axios 前缀，
 * 例如 <img src="/dev-api/profile/upload/...">。当前应用的接口代理前缀是 apiURL
 * （dev 为 /api），/dev-api 路径经 Vite 会走 SPA 回退返回 index.html，导致图片不显示。
 * 渲染/编辑前把旧前缀重写为当前 apiURL。
 */
export function normalizeContentHtml(html = ''): string {
  if (!html) return '';
  const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);
  const base = (apiURL || '').replace(/\/$/, '');
  // 仅重写 src/href 属性值里的旧接口前缀，避免误伤正文文本
  return html.replace(
    /((?:src|href)\s*=\s*["'])\/(?:dev-api|prod-api|stage-api)\//g,
    `$1${base}/`,
  );
}
