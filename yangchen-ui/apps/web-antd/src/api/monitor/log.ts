import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

export interface SysOperLog {
  businessType?: number | string;
  costTime?: number;
  deptName?: string;
  errorMsg?: string;
  jsonResult?: string;
  method?: string;
  operId?: number | string;
  operIp?: string;
  operLocation?: string;
  operName?: string;
  operParam?: string;
  operTime?: string;
  operUrl?: string;
  requestMethod?: string;
  status?: string;
  title?: string;

  [key: string]: any;
}

export interface SysLogininfor {
  browser?: string;
  infoId?: number | string;
  ipaddr?: string;
  loginLocation?: string;
  loginTime?: string;
  msg?: string;
  os?: string;
  status?: string;
  userName?: string;

  [key: string]: any;
}

export const listOperLogApi = (params: Recordable<any>) =>
  requestClient.get<{ rows: SysOperLog[]; total: number }>('/monitor/operlog/list', {params});

export const deleteOperLogApi = (ids: (number | string)[]) =>
  requestClient.delete(`/monitor/operlog/${ids.join(',')}`);

export const cleanOperLogApi = () => requestClient.delete('/monitor/operlog/clean');

export const listLoginLogApi = (params: Recordable<any>) =>
  requestClient.get<{ rows: SysLogininfor[]; total: number }>('/monitor/logininfor/list', {params});

export const deleteLoginLogApi = (ids: (number | string)[]) =>
  requestClient.delete(`/monitor/logininfor/${ids.join(',')}`);

export const cleanLoginLogApi = () => requestClient.delete('/monitor/logininfor/clean');

export const unlockLoginApi = (userName: string) =>
  requestClient.get(`/monitor/logininfor/unlock/${encodeURIComponent(userName)}`);
