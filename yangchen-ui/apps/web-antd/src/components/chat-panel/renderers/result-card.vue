<script lang="ts" setup>
import {computed} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {displayValue, getUiData, statusClass} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatResultCard'});

const props = defineProps<{message: ChatMessage}>();
const data = computed(() => getUiData(props.message));
const items = computed(() =>
  Array.isArray(data.value.items) ? data.value.items : [],
);
</script>

<template>
  <section class="ui-card ui-result-card">
    <div class="ui-result-card__icon"><IconifyIcon icon="lucide:check-check"/></div>
    <div class="ui-result-card__body">
      <div class="ui-result-card__title-row">
        <div class="ui-card__title">{{ data.title || '处理完成' }}</div>
        <span v-if="data.status" :class="['ui-status', statusClass(data.status)]">
          {{ data.status }}
        </span>
      </div>
      <div v-if="data.message" class="ui-card__desc">{{ data.message }}</div>
      <div v-if="items.length" class="ui-result-list">
        <div v-for="(item, index) in items" :key="index" class="ui-result-item">
          <span>{{ item.label || item.key }}</span>
          <strong>{{ displayValue(item.value) }}</strong>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ui-card {
  display: flex;
  width: 100%;
  gap: 13px;
  padding: 16px;
  border: 1px solid hsl(var(--success) / 0.28);
  border-radius: 16px;
  background: linear-gradient(135deg, hsl(var(--success) / 0.06), hsl(var(--card) / 0.94));
}

.ui-result-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  color: hsl(var(--success));
  border-radius: 10px;
  background: hsl(var(--success) / 0.12);
}

.ui-result-card__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.ui-result-card__body {
  min-width: 0;
  flex: 1;
}

.ui-result-card__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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

.ui-status {
  padding: 3px 7px;
  font-size: 11px;
  border-radius: 6px;
  background: hsl(var(--muted));
}

.ui-status.is-success {
  color: hsl(var(--success));
  background: hsl(var(--success) / 0.1);
}

.ui-status.is-danger {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
}

.ui-result-list {
  display: grid;
  gap: 7px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid hsl(var(--border) / 0.7);
}

.ui-result-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.ui-result-item span {
  color: hsl(var(--muted-foreground));
}

.ui-result-item strong {
  font-weight: 550;
  text-align: right;
}
</style>
