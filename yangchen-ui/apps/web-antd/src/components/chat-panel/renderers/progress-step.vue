<script lang="ts" setup>
import {IconifyIcon} from '@vben/icons';

import type {ChatMessage} from '../types';

defineOptions({name: 'ProgressStep'});

defineProps<{message: ChatMessage}>();

/** 进度旁白：工具执行前后的实时状态提示，仅当前轮展示，不落历史 */
function stepText(message: ChatMessage) {
  const ui = message.extra?.ui as {data?: {text?: string}} | undefined;
  return ui?.data?.text || message.content || '…';
}
</script>

<template>
  <div class="ps-step">
    <span class="ps-step__icon">
      <IconifyIcon icon="lucide:loader-circle" class="ps-step__spinner"/>
    </span>
    <span class="ps-step__text">{{ stepText(message) }}</span>
  </div>
</template>

<style scoped>
.ps-step {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 5px 12px;
  font-size: 13px;
  line-height: 1.6;
  color: hsl(var(--muted-foreground));
  border: 1px solid hsl(var(--border) / 0.62);
  border-radius: 999px;
  background: hsl(var(--muted) / 0.5);
}

.ps-step__icon {
  display: inline-flex;
  flex-shrink: 0;
  color: hsl(var(--primary));
}

.ps-step__icon :deep(svg) {
  width: 13px;
  height: 13px;
}

.ps-step__spinner {
  animation: ps-spin 1.6s linear infinite;
}

.ps-step__text {
  min-width: 0;
  word-break: break-word;
}

@keyframes ps-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .ps-step__spinner {
    animation: none;
  }
}
</style>
