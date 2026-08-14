/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type {RequestClientOptions} from '@vben/request';
import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';

import {useAppConfig} from '@vben/hooks';
import {preferences} from '@vben/preferences';
import {useAccessStore} from '@vben/stores';

import {message} from 'ant-design-vue';

import {useAuthStore} from '#/store';

import {refreshTokenApi} from './core';

const {apiURL} = useAppConfig(import.meta.env, import.meta.env.PROD);

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  /**
   * 重新认证逻辑
   */
  async function doReAuthenticate() {
    console.warn('Access token or refresh token is invalid or expired. ');
    const accessStore = useAccessStore();
    const authStore = useAuthStore();
    accessStore.setAccessToken(null);
    if (
      preferences.app.loginExpiredMode === 'modal' &&
      accessStore.isAccessChecked
    ) {
      accessStore.setLoginExpired(true);
    } else {
      await authStore.logout();
    }
  }

  /**
   * 刷新token逻辑
   */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const resp = await refreshTokenApi();
    const newToken = resp.data;
    accessStore.setAccessToken(newToken);
    return newToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  // 请求头处理
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();

      config.headers.Authorization = formatToken(accessStore.accessToken);
      config.headers['Accept-Language'] = preferences.app.locale;
      return config;
    },
  });

  // 处理返回的响应数据格式
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      // RuoYi 标准接口有 data 字段走 data；/login、/getInfo 等顶层字段接口返回整个 body
      dataField: (response) => response.data ?? response,
      successCode: 200,
    }),
  );

  // token过期的处理
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: preferences.app.enableRefreshToken,
      formatToken,
    }),
  );

  // RuoYi 会话失效处理：后端对无效/过期 token 返回 HTTP 200 + body code 401（区别于标准 HTTP 401）
  client.addResponseInterceptor({
    rejected: async (error) => {
      const {config, response} = error;
      // 仅处理 RuoYi 业务 401；标准 HTTP 401 已由上面的 authenticateResponseInterceptor 处理
      if (response?.status !== 200 || response?.data?.code !== 401) {
        throw error;
      }
      // 避免重复触发重新登录
      if (config.__isRetryRequest) {
        throw error;
      }
      config.__isRetryRequest = true;
      await doReAuthenticate();
      throw error;
    },
  });

  // 通用的错误处理,如果没有进入上面的错误处理逻辑，就会进入这里
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string, error) => {
      // 这里可以根据业务进行定制,你可以拿到 error 内的信息进行定制化处理，根据不同的 code 做不同的提示，而不是直接使用 message.error 提示 msg
      // 当前接口返回的错误字段是 msg 或者 error 或者 message（RuoYi 为 msg）
      const responseData = error?.response?.data ?? {};
      const errorMessage =
        responseData?.msg ?? responseData?.error ?? responseData?.message ?? '';
      // 如果没有错误信息，则会根据状态码进行提示
      message.error(errorMessage || msg);
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});

export const baseRequestClient = new RequestClient({baseURL: apiURL});
