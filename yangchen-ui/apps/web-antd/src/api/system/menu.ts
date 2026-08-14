import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

import type {TreeSelectNode} from './role';

export interface SysMenu {
  alwaysShow?: string;
  children?: SysMenu[];
  component?: string;
  icon?: string;
  isCache?: string;
  isFrame?: string;
  menuId?: number | string;
  menuName?: string;
  menuType?: 'C' | 'F' | 'M';
  orderNum?: number;
  parentId?: number | string;
  path?: string;
  perms?: string;
  query?: string;
  routeName?: string;
  status?: string;
  visible?: string;

  [key: string]: any;
}

/** 菜单列表 */
export async function listMenuApi(params?: Recordable<any>) {
  return requestClient.get<SysMenu[]>('/system/menu/list', {params});
}

/** 菜单详情 */
export async function getMenuApi(menuId: number | string) {
  return requestClient.get<SysMenu>(`/system/menu/${menuId}`);
}

/** 新增菜单 */
export async function addMenuApi(data: SysMenu) {
  return requestClient.post('/system/menu', data);
}

/** 修改菜单 */
export async function updateMenuApi(data: SysMenu) {
  return requestClient.put('/system/menu', data);
}

/** 保存菜单排序 */
export async function updateMenuSortApi(data: { menuIds: string; orderNums: string }) {
  return requestClient.put('/system/menu/updateSort', data);
}

/** 删除菜单 */
export async function deleteMenuApi(menuId: number | string) {
  return requestClient.delete(`/system/menu/${menuId}`);
}

/** 菜单下拉树（角色分配菜单权限用） */
export async function getMenuTreeSelectApi() {
  return requestClient.get<TreeSelectNode[]>('/system/menu/treeselect');
}

/** 根据角色ID查询菜单树结构 */
export async function getRoleMenuTreeSelectApi(roleId: number) {
  return requestClient.get<{ checkedKeys: number[]; menus: TreeSelectNode[] }>(
    `/system/menu/roleMenuTreeselect/${roleId}`,
  );
}
