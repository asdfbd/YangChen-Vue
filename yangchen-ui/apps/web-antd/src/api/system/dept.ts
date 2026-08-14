import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

import type {TreeSelectNode} from './role';

/** RuoYi SysDept */
export interface SysDept {
  children?: SysDept[];
  createTime?: string;
  deptId?: number | string;
  deptName?: string;
  email?: string;
  leader?: string;
  orderNum?: number;
  parentId?: number | string;
  parentName?: string;
  phone?: string;
  status?: string;

  [key: string]: any;
}

/** 部门列表（树形数据由页面按 deptId 组织） */
export async function listDeptApi(params?: Recordable<any>) {
  return requestClient.get<SysDept[]>('/system/dept/list', {params});
}

/** 部门详情 */
export async function getDeptApi(deptId: number | string) {
  return requestClient.get<SysDept>(`/system/dept/${deptId}`);
}

/** 新增部门 */
export async function addDeptApi(data: SysDept) {
  return requestClient.post('/system/dept', data);
}

/** 修改部门 */
export async function updateDeptApi(data: SysDept) {
  return requestClient.put('/system/dept', data);
}

/** 保存部门排序 */
export async function updateDeptSortApi(data: {
  deptIds: string;
  orderNums: string;
}) {
  return requestClient.put('/system/dept/updateSort', data);
}

/** 删除部门 */
export async function deleteDeptApi(deptId: number | string) {
  return requestClient.delete(`/system/dept/${deptId}`);
}

/** 上级部门选择树；编辑时自动排除当前节点及其子节点 */
export async function getDeptTreeSelectApi(deptId?: number | string) {
  const path = deptId === undefined ? '/system/dept/list' : `/system/dept/list/exclude/${deptId}`;
  return requestClient.get<SysDept[] | TreeSelectNode[]>(path);
}
