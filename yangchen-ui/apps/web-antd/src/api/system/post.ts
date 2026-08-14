import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

/** RuoYi SysPost（岗位） */
export interface SysPost {
  createBy?: string;
  createTime?: string;
  postCode?: string;
  postId?: number;
  postName?: string;
  postSort?: number;
  remark?: string;
  status?: string;

  [key: string]: any;
}

/** 岗位列表 */
export async function listPostApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysPost[]; total: number }>(
    '/system/post/list',
    {params},
  );
}

/** 岗位详情 */
export async function getPostApi(postId: number | string) {
  return requestClient.get<SysPost>(`/system/post/${postId}`);
}

/** 新增岗位 */
export async function addPostApi(data: SysPost) {
  return requestClient.post('/system/post', data);
}

/** 修改岗位 */
export async function updatePostApi(data: SysPost) {
  return requestClient.put('/system/post', data);
}

/** 删除岗位（支持逗号分隔批量） */
export async function deletePostApi(postIds: (number | string)[]) {
  return requestClient.delete(`/system/post/${postIds.join(',')}`);
}

/** 岗位选择框列表 */
export async function optionSelectPostApi() {
  return requestClient.get<SysPost[]>('/system/post/optionselect');
}
