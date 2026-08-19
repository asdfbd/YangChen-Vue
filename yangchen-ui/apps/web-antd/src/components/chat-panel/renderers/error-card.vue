<script lang="ts" setup>
import {computed} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {getUiData, getUiPayload} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatErrorCard'});

const props = defineProps<{message: ChatMessage}>();
const emit = defineEmits<{
  'ui-action': [payload: {action: 'submit'; actionId?: string}];
}>();

const data = computed(() => getUiData(props.message));
const payload = computed(() => getUiPayload(props.message));
</script>

<template>
  <section class="ui-card ui-error-card">
    <div class="ui-error-card__icon"><IconifyIcon icon="lucide:triangle-alert"/></div>
    <div class="ui-error-card__body">
      <div class="ui-card__title">{{ data.title || '处理失败' }}</div>
      <div class="ui-card__desc">{{ data.message || data.description || '请求未能完成，请稍后重试。' }}</div>
      <button
        v-if="payload?.action?.actionId"
        class="ui-retry"
        type="button"
        @click="emit('ui-action', {action: 'submit', actionId: payload.action.actionId})"
      >
        <IconifyIcon icon="lucide:rotate-cw"/>重试
      </button>
    </div>
  </section>
</template>

<style scoped>
.ui-card {
  display: flex;
  width: 100%;
  gap: 13px;
  padding: 16px;
  border: 1px solid hsl(var(--destructive) / 0.25);
  border-radius: 16px;
  background: hsl(var(--destructive) / 0.045);
}

.ui-error-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  color: hsl(var(--destructive));
  border-radius: 10px;
  background: hsl(var(--destructive) / 0.1);
}

.ui-error-card__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.ui-error-card__body {
  min-width: 0;
  flex: 1;
}

.ui-card__title {
  font-size: 15px;
  font-weight: 650;
}

.ui-card__desc {
  margin-top: 5px;
  font-size: 13px;
  line-height: 1.65;
  color: hsl(var(--muted-foreground));
}

.ui-retry {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 34px;
  margin-top: 12px;
  padding: 0 11px;
  font-size: 12px;
  color: hsl(var(--destructive));
  border: 1px solid hsl(var(--destructive) / 0.25);
  border-radius: 8px;
  background: hsl(var(--card) / 0.75);
  cursor: pointer;
}

.ui-retry :deep(svg) {
  width: 14px;
  height: 14px;
}
</style>
