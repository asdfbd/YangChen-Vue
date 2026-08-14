<script lang="ts" setup>
import type {VbenFormSchema} from '@vben/common-ui';
import {AuthenticationLogin, z} from '@vben/common-ui';
import type {Recordable} from '@vben/types';

import {computed, markRaw, onMounted, ref} from 'vue';
import {$t} from '@vben/locales';

import {getCaptchaImageApi} from '#/api';
import {useAuthStore} from '#/store';

import CaptchaField from './captcha-field.vue';

defineOptions({name: 'Login'});

const authStore = useAuthStore();

interface CaptchaState {
  captchaEnabled: boolean;
  img: string;
  uuid: string;
}

/** 后端验证码状态 */
const captcha = ref<CaptchaState | null>(null);

async function refreshCaptcha() {
  captcha.value = await getCaptchaImageApi();
}

onMounted(refreshCaptcha);

const formSchema = computed((): VbenFormSchema[] => {
  const schema: VbenFormSchema[] = [
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z.string().min(1, {message: $t('authentication.usernameTip')}),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, {message: $t('authentication.passwordTip')}),
    },
  ];

  // 后端开启验证码时展示图片验证码
  if (captcha.value?.captchaEnabled) {
    schema.push({
      component: markRaw(CaptchaField),
      componentProps: {
        img: `data:image/jpeg;base64,${captcha.value?.img ?? ''}`,
        onRefresh: refreshCaptcha,
      },
      fieldName: 'code',
      label: $t('authentication.code'),
      rules: z.string().min(1, {message: $t('authentication.code')}),
    });
  }

  return schema;
});

async function handleSubmit(values: Recordable<any>) {
  try {
    await authStore.authLogin({
      ...values,
      // 验证码禁用时不携带 code/uuid
      ...(captcha.value?.captchaEnabled ? {uuid: captcha.value?.uuid} : {}),
    });
  } catch {
    // 登录失败后刷新验证码
    if (captcha.value?.captchaEnabled) {
      await refreshCaptcha();
    }
  }
}
</script>

<template>
  <AuthenticationLogin
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    @submit="handleSubmit"
  />
</template>
