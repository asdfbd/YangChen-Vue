import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

/** RuoYi SysConfig */
export interface SysConfig {
  configId?: number | string;
  configKey?: string;
  configName?: string;
  configType?: string;
  configValue?: string;
  createTime?: string;
  remark?: string;

  [key: string]: any;
}

/** 参数列表 */
export async function listConfigApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysConfig[]; total: number }>(
    '/system/config/list',
    {params},
  );
}

/** 参数详情 */
export async function getConfigApi(configId: number | string) {
  return requestClient.get<SysConfig>(`/system/config/${configId}`);
}

/** 新增参数 */
export async function addConfigApi(data: SysConfig) {
  return requestClient.post('/system/config', data);
}

/** 修改参数 */
export async function updateConfigApi(data: SysConfig) {
  return requestClient.put('/system/config', data);
}

/** 删除参数 */
export async function deleteConfigApi(configIds: (number | string)[]) {
  return requestClient.delete(`/system/config/${configIds.join(',')}`);
}

/** 刷新参数缓存 */
export async function refreshConfigCacheApi() {
  return requestClient.delete('/system/config/refreshCache');
}
