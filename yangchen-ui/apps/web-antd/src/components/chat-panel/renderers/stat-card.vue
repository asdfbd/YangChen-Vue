<script lang="ts" setup>
import {computed} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {displayValue, getUiData} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatStatCard'});

const props = defineProps<{message: ChatMessage}>();
const data = computed(() => getUiData(props.message));
</script>

<template>
  <section class="ui-stat-card">
    <div class="ui-stat-card__icon"><IconifyIcon icon="lucide:chart-no-axes-column"/></div>
    <div class="ui-stat-card__body">
      <div class="ui-stat-card__title">{{ data.title || '查询结果' }}</div>
      <div class="ui-stat-card__value">{{ displayValue(data.value) }}</div>
      <div class="ui-stat-card__label">{{ data.label || '数量' }}</div>
    </div>
  </section>
</template>

<style scoped>
.ui-stat-card {
  display: flex;
  width: min(100%, 320px);
  gap: 14px;
  padding: 17px 18px;
  border: 1px solid hsl(var(--primary) / 0.18);
  border-radius: 16px;
  background: linear-gradient(135deg, hsl(var(--primary) / 0.1), hsl(var(--card) / 0.94));
  box-shadow: 0 12px 28px -24px hsl(var(--primary) / 0.65);
}

.ui-stat-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  color: hsl(var(--primary));
  border-radius: 11px;
  background: hsl(var(--primary) / 0.13);
}

.ui-stat-card__icon :deep(svg) {
  width: 19px;
  height: 19px;
}

.ui-stat-card__body {
  min-width: 0;
}

.ui-stat-card__title {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}

.ui-stat-card__value {
  margin-top: 2px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: hsl(var(--foreground));
}

.ui-stat-card__label {
  margin-top: 3px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}
</style>
