<script lang="ts" setup>
import {computed} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {displayValue, getUiData, statusClass} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatDetailCard'});

const props = defineProps<{message: ChatMessage}>();
const data = computed(() => getUiData(props.message));
const fields = computed(() =>
  Array.isArray(data.value.fields) ? data.value.fields : [],
);
</script>

<template>
  <section class="ui-card ui-detail-card">
    <div v-if="data.title || data.description" class="ui-card__head">
      <div>
        <div v-if="data.title" class="ui-card__title">{{ data.title }}</div>
        <div v-if="data.description" class="ui-card__desc">
          {{ data.description }}
        </div>
      </div>
      <span class="ui-card__mark"><IconifyIcon icon="lucide:circle-user-round"/></span>
    </div>
    <div v-if="fields.length" class="ui-detail-grid">
      <div v-for="(field, index) in fields" :key="String(field.key ?? index)" class="ui-detail-item">
        <span class="ui-detail-item__label">{{ field.label || field.key }}</span>
        <span
          :class="[
            'ui-detail-item__value',
            field.valueType === 'status' && statusClass(field.value),
          ]"
        >
          {{ displayValue(field.value) }}
        </span>
      </div>
    </div>
    <div v-else class="ui-card__empty">暂无可展示的数据</div>
  </section>
</template>

<style scoped>
.ui-card {
  width: 100%;
  overflow: hidden;
  border: 1px solid hsl(var(--border));
  border-radius: 16px;
  background: hsl(var(--card) / 0.9);
  box-shadow: 0 10px 28px -24px rgba(15, 23, 42, 0.55);
}

.ui-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px 13px;
  border-bottom: 1px solid hsl(var(--border) / 0.75);
}

.ui-card__title {
  font-size: 15px;
  font-weight: 650;
  line-height: 1.4;
}

.ui-card__desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: hsl(var(--muted-foreground));
}

.ui-card__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  color: hsl(var(--primary));
  border-radius: 9px;
  background: hsl(var(--primary) / 0.1);
}

.ui-card__mark :deep(svg) {
  width: 16px;
  height: 16px;
}

.ui-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.ui-detail-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
  padding: 13px 18px;
  border-bottom: 1px solid hsl(var(--border) / 0.65);
}

.ui-detail-item:nth-child(odd) {
  border-right: 1px solid hsl(var(--border) / 0.65);
}

.ui-detail-item:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.ui-detail-item__label {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
}

.ui-detail-item__value {
  overflow: hidden;
  font-size: 14px;
  font-weight: 550;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-detail-item__value.is-success,
.ui-detail-item__value.is-danger {
  color: hsl(var(--primary));
}

.ui-detail-item__value.is-danger {
  color: hsl(var(--destructive));
}

.ui-card__empty {
  padding: 20px 18px;
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}

@media (max-width: 640px) {
  .ui-detail-grid {
    grid-template-columns: 1fr;
  }

  .ui-detail-item:nth-child(odd) {
    border-right: 0;
  }

  .ui-detail-item:nth-last-child(-n + 2) {
    border-bottom: 1px solid hsl(var(--border) / 0.65);
  }

  .ui-detail-item:last-child {
    border-bottom: 0;
  }
}
</style>
