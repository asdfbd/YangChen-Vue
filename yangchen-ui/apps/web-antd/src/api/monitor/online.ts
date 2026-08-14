import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

export interface OnlineUser {
  deptName?: string;
  ipaddr?: string;
  loginLocation?: string;
  loginTime?: string;
  os?: string;
  status?: string;
  tokenId?: string;
  userName?: string;
}

export const listOnlineUserApi = (params: Recordable<any>) =>
  requestClient.get<{ rows: OnlineUser[]; total: number }>('/monitor/online/list', {params});

export const forceLogoutApi = (tokenId: string) =>
  requestClient.delete(`/monitor/online/${encodeURIComponent(tokenId)}`);
