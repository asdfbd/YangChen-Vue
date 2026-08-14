import type {UserInfo} from '@vben/types';

import {useAppConfig} from '@vben/hooks';

import {requestClient} from '#/api/request';

/** /getInfo 响应中的用户对象（RuoYi SysUser） */
interface RuoYiUser {
  avatar?: string;
  nickName?: string;
  userId?: number | string;
  userName?: string;

  [key: string]: any;
}

/** /getInfo 响应 */
interface GetInfoResult {
  permissions: string[];
  roles: string[];
  user: RuoYiUser;
}

/** 用户信息接口返回（userInfo 已映射为 Vben UserInfo） */
interface UserInfoResult {
  permissions: string[];
  roles: string[];
  userInfo: UserInfo;
}

/**
 * 获取用户信息（含角色与权限码）
 */
export async function getUserInfoApi(): Promise<UserInfoResult> {
  const resp = await requestClient.get<GetInfoResult>('/getInfo');
  const {user, roles, permissions} = resp;
  const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);

  const userInfo: UserInfo = {
    // RuoYi 头像为相对路径 /profile/...，dev 下经 /api 代理可达
    avatar: user.avatar ? `${apiURL}${user.avatar}` : '',
    desc: '',
    homePath: '',
    realName: user.nickName ?? '',
    roles,
    token: '',
    userId: String(user.userId ?? ''),
    username: user.userName ?? '',
  };

  return {permissions, roles, userInfo};
}
