import {baseRequestClient, requestClient} from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    /** 验证码 */
    code?: string;
    /** 密码 */
    password?: string;
    /** 用户名 */
    username?: string;
    /** 验证码 uuid */
    uuid?: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
  }

  export interface RefreshTokenResult {
    data: string;
    status: number;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const resp = await requestClient.post<{ token: string }>('/login', data);
  return {accessToken: resp.token};
}

/**
 * 刷新accessToken（RuoYi 无刷新接口，enableRefreshToken=false 时不会调用；保留以满足 request 层引用）
 */
export async function refreshTokenApi() {
  return baseRequestClient.post<AuthApi.RefreshTokenResult>('/auth/refresh', {
    withCredentials: true,
  });
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return requestClient.post('/logout');
}
