import type {Recordable} from '@vben/types';

import {useAppConfig} from '@vben/hooks';
import {useAccessStore} from '@vben/stores';

/**
 * 导出文件（blob 下载）。
 *
 * RuoYi 的 /export 接口返回 xlsx 文件流，而 requestClient 是 JSON 信封客户端
 * （defaultResponseInterceptor 只认 code===200 的 JSON），故这里用 fetch 直连
 * 并附带鉴权头；后端业务错误（如无权限）返回 HTTP 200 + JSON 信封时抛出提示。
 */
export async function downloadBlob(options: {
  body?: Recordable<any>;
  filename?: string;
  method?: 'GET' | 'POST';
  path: string;
}) {
  const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);
  const accessStore = useAccessStore();

  const {body, filename, method = 'POST', path} = options;

  const res = await fetch(`${apiURL}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${accessStore.accessToken}`,
      ...(body ? {'Content-Type': 'application/json'} : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }

  const contentType = res.headers.get('Content-Type') ?? '';
  // 业务错误：导出失败返回 JSON 信封（HTTP 200）
  if (contentType.includes('application/json')) {
    const data = await res.json();
    throw new Error(data?.msg ?? data?.message ?? '导出失败');
  }

  const blob = await res.blob();
  const disposition = res.headers.get('Content-Disposition') ?? '';
  const match = disposition.match(/filename="?([^";]+)"?/);
  const name =
    filename ??
    (match?.[1]
      ? decodeURIComponent(match[1])
      : `export_${Date.now()}.xlsx`);

  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}
