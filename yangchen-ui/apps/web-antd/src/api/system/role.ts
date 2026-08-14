import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

import type {SysUser} from './user';

/** RuoYi TreeSelect 树节点（/system/menu/treeselect、/system/role/deptTree、/system/user/deptTree 返回） */
export interface TreeSelectNode {
  children?: TreeSelectNode[];
  disabled?: boolean;
  /** 后端 Long 序列化为字符串，因此可能是 string */
  id: number | string;
  label: string;
}

/** RuoYi SysRole */
export interface SysRole {
  createTime?: string;
  dataScope?: string;
  deptCheckStrictly?: boolean;
  deptIds?: number[];
  menuCheckStrictly?: boolean;
  menuIds?: number[];
  remark?: string;
  roleId?: number;
  roleKey?: string;
  roleName?: string;
  roleSort?: number;
  status?: string;

  [key: string]: any;
}

/** 角色列表 */
export async function listRoleApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysRole[]; total: number }>(
    '/system/role/list',
    {params},
  );
}

/** 角色详情 */
export async function getRoleApi(roleId: number) {
  return requestClient.get<SysRole>(`/system/role/${roleId}`);
}

/** 新增角色 */
export async function addRoleApi(data: SysRole) {
  return requestClient.post('/system/role', data);
}

/** 修改角色 */
export async function updateRoleApi(data: SysRole) {
  return requestClient.put('/system/role', data);
}

/** 修改数据权限 */
export async function dataScopeApi(data: SysRole) {
  return requestClient.put('/system/role/dataScope', data);
}

/** 修改角色状态 */
export async function changeRoleStatusApi(roleId: number, status: string) {
  return requestClient.put('/system/role/changeStatus', {roleId, status});
}

/** 删除角色（支持逗号分隔批量） */
export async function deleteRoleApi(roleIds: number[]) {
  return requestClient.delete(`/system/role/${roleIds.join(',')}`);
}

/** 角色部门树（数据权限） */
export async function deptTreeSelectApi(roleId: number) {
  return requestClient.get<{ checkedKeys: number[]; depts: TreeSelectNode[] }>(
    `/system/role/deptTree/${roleId}`,
  );
}

/** 已分配用户列表 */
export async function allocatedUserListApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysUser[]; total: number }>(
    '/system/role/authUser/allocatedList',
    {params},
  );
}

/** 未分配用户列表 */
export async function unallocatedUserListApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysUser[]; total: number }>(
    '/system/role/authUser/unallocatedList',
    {params},
  );
}

/** 取消用户授权（body） */
export async function cancelAuthUserApi(data: {
  roleId: string | number;
  userId: string | number;
}) {
  return requestClient.put('/system/role/authUser/cancel', data);
}

/** 批量取消用户授权（query 参数） */
export async function cancelAuthUserAllApi(params: Recordable<any>) {
  return requestClient.put('/system/role/authUser/cancelAll', undefined, {
    params,
  });
}

/** 批量选择用户授权（query 参数） */
export async function selectAuthUserAllApi(params: Recordable<any>) {
  return requestClient.put('/system/role/authUser/selectAll', undefined, {
    params,
  });
}
