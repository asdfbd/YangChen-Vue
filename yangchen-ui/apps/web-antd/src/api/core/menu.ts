import type {RouteRecordStringComponent} from '@vben/types';

import {requestClient} from '#/api/request';

/**
 * 获取当前用户的 Vben 动态路由树。
 * 后端直接返回 RouteRecordStringComponent 协议，不做 RuoYi 兼容转换。
 */
export async function getAllMenusApi() {
  return requestClient.get<RouteRecordStringComponent[]>('/getRouters');
}
