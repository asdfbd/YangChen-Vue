import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

export interface SysDictType {
  createTime?: string;
  dictId?: number | string;
  dictName?: string;
  dictType?: string;
  remark?: string;
  status?: string;

  [key: string]: any;
}

export interface SysDictData {
  createTime?: string;
  cssClass?: string;
  dictCode?: number | string;
  dictLabel?: string;
  dictSort?: number;
  dictType?: string;
  dictValue?: string;
  listClass?: string;
  remark?: string;
  status?: string;

  [key: string]: any;
}

export async function listDictTypeApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysDictType[]; total: number }>(
    '/system/dict/type/list',
    {params},
  );
}

export async function getDictTypeApi(dictId: number | string) {
  return requestClient.get<SysDictType>(`/system/dict/type/${dictId}`);
}

export async function addDictTypeApi(data: SysDictType) {
  return requestClient.post('/system/dict/type', data);
}

export async function updateDictTypeApi(data: SysDictType) {
  return requestClient.put('/system/dict/type', data);
}

export async function deleteDictTypeApi(dictIds: (number | string)[]) {
  return requestClient.delete(`/system/dict/type/${dictIds.join(',')}`);
}

export async function refreshDictCacheApi() {
  return requestClient.delete('/system/dict/type/refreshCache');
}

export async function getDictTypeOptionsApi() {
  return requestClient.get<SysDictType[]>('/system/dict/type/optionselect');
}

export async function listDictDataApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysDictData[]; total: number }>(
    '/system/dict/data/list',
    {params},
  );
}

export async function getDictDataApi(dictCode: number | string) {
  return requestClient.get<SysDictData>(`/system/dict/data/${dictCode}`);
}

export async function addDictDataApi(data: SysDictData) {
  return requestClient.post('/system/dict/data', data);
}

export async function updateDictDataApi(data: SysDictData) {
  return requestClient.put('/system/dict/data', data);
}

export async function deleteDictDataApi(dictCodes: (number | string)[]) {
  return requestClient.delete(`/system/dict/data/${dictCodes.join(',')}`);
}
