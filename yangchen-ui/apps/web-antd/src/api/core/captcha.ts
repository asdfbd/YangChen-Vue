import {requestClient} from '#/api/request';

/** /captchaImage 响应 */
export interface CaptchaImageResult {
  captchaEnabled: boolean;
  img: string;
  uuid: string;
}

/**
 * 获取验证码图片
 */
export async function getCaptchaImageApi() {
  return requestClient.get<CaptchaImageResult>('/captchaImage');
}
