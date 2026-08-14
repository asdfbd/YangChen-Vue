<script lang="ts" setup>
import {Input} from 'ant-design-vue';

import {$t} from '@vben/locales';

defineOptions({name: 'CaptchaField'});

const props = defineProps<{
  /** 验证码图片（dataURL） */
  img?: string;
  /** 验证码输入值 */
  modelValue?: string;
  /** 点击图片刷新验证码 */
  onRefresh?: () => void;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<template>
  <div class="flex w-full items-center gap-2">
    <Input
      :placeholder="$t('authentication.code')"
      :value="modelValue"
      class="flex-1"
      @update:value="emit('update:modelValue', $event)"
    />
    <img
      :src="img"
      alt="captcha"
      class="h-9 w-28 shrink-0 cursor-pointer rounded object-cover"
      @click="onRefresh"
    />
  </div>
</template>
