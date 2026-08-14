import {requestClient} from '#/api/request';

export interface CacheInfo {
  commandStats?: Array<{ name?: string; value?: number }>;
  dbSize?: number;
  info?: Record<string, string>;
}

/**
 * 缓存名称列表项，对应后端 SysCache：
 * cacheName 为键前缀（如 login_tokens:），remark 为中文备注
 */
export interface CacheName {
  cacheName?: string;
  cacheKey?: string;
  cacheValue?: string;
  remark?: string;
}

export const getCacheApi = () => requestClient.get<CacheInfo>('/monitor/cache');
export const listCacheNameApi = () => requestClient.get<CacheName[]>('/monitor/cache/getNames');
export const listCacheKeyApi = (cacheName: string) => requestClient.get<string[]>(`/monitor/cache/getKeys/${encodeURIComponent(cacheName)}`);
export const getCacheValueApi = (cacheName: string, cacheKey: string) => requestClient.get<CacheName>(`/monitor/cache/getValue/${encodeURIComponent(cacheName)}/${encodeURIComponent(cacheKey)}`);
export const clearCacheNameApi = (cacheName: string) => requestClient.delete(`/monitor/cache/clearCacheName/${encodeURIComponent(cacheName)}`);
export const clearCacheKeyApi = (cacheKey: string) => requestClient.delete(`/monitor/cache/clearCacheKey/${encodeURIComponent(cacheKey)}`);
export const clearCacheAllApi = () => requestClient.delete('/monitor/cache/clearCacheAll');
