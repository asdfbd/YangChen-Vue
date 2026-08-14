import type {Recordable} from '@vben/types';

import {requestClient} from '#/api/request';

import type {SysPost} from './post';
import type {SysRole, TreeSelectNode} from './role';

/** RuoYi SysUser */
export interface SysUser {
  createTime?: string;
  dept?: { deptName?: string };
  deptId?: number;
  email?: string;
  nickName?: string;
  password?: string;
  phonenumber?: string;
  postIds?: number[];
  remark?: string;
  roleIds?: number[];
  sex?: string;
  status?: string;
  userId?: number;
  userName?: string;

  [key: string]: any;
}

/** GET /system/user/{userId} 返回结构 */
export interface SysUserInfo {
  data: SysUser;
  postIds: number[];
  roleIds: number[];
  roles: SysRole[];
  posts: SysPost[];
}

/** 用户列表 */
export async function getUserListApi(params: Recordable<any>) {
  return requestClient.get<{ rows: SysUser[]; total: number }>(
    '/system/user/list',
    {params},
  );
}

/**
 * 用户详情（不带 userId 时用于新增场景：仅返回角色/岗位选项）。
 * 该接口需要完整的信封（data + roles/posts 选项 + postIds/roleIds），
 * 因此用 responseReturn: 'body' 跳过默认的 data 字段解包。
 */
export async function getUserApi(userId?: number) {
  return requestClient.get<SysUserInfo>(
    userId ? `/system/user/${userId}` : '/system/user/',
    {responseReturn: 'body'},
  );
}

/** 部门树（RuoYi TreeSelect 结构 id/label/children/disabled） */
export async function getDeptTreeApi() {
  return requestClient.get<TreeSelectNode[]>('/system/user/deptTree');
}

/** 新增用户 */
export async function addUserApi(data: SysUser) {
  return requestClient.post('/system/user', data);
}

/** 修改用户 */
export async function updateUserApi(data: SysUser) {
  return requestClient.put('/system/user', data);
}

/** 删除用户（支持逗号分隔批量） */
export async function deleteUserApi(userIds: (number | string)[]) {
  return requestClient.delete(`/system/user/${userIds.join(',')}`);
}

/** 重置密码 */
export async function resetUserPwdApi(userId: number | string, password: string) {
  return requestClient.put('/system/user/resetPwd', {userId, password});
}

/** 修改状态 */
export async function changeUserStatusApi(userId: number | string, status: string) {
  return requestClient.put('/system/user/changeStatus', {userId, status});
}

/** 个人中心 - 查询当前登录用户个人信息（data + roleGroup/postGroup 平级，需 body 完整解包） */
export interface UserProfileResult {
  data: SysUser;
  postGroup: string;
  roleGroup: string;
}

/** 查询个人信息 */
export async function getUserProfileApi() {
  return requestClient.get<UserProfileResult>('/system/user/profile', {
    responseReturn: 'body',
  });
}

/** 修改个人信息（nickName / phonenumber / email / sex） */
export async function updateUserProfileApi(data: Partial<SysUser>) {
  return requestClient.put('/system/user/profile', data);
}

/** 修改密码 */
export async function updateUserPwdApi(oldPassword: string, newPassword: string) {
  return requestClient.put('/system/user/profile/updatePwd', {
    oldPassword,
    newPassword,
  });
}

/** 上传头像（multipart，字段名 avatarfile） */
export async function uploadAvatarApi(data: FormData) {
  return requestClient.post<{ imgUrl: string }>(
    '/system/user/profile/avatar',
    data,
  );
}
