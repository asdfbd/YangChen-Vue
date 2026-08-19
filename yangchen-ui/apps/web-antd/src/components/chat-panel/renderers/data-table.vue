<script lang="ts" setup>
import {computed} from 'vue';

import {IconifyIcon} from '@vben/icons';

import {fieldLabel} from '../field-labels';

import {displayValue, getUiData, isRecord} from './ui-types';

import type {ChatMessage} from '../types';

defineOptions({name: 'ChatDataTable'});

const props = defineProps<{message: ChatMessage}>();
const data = computed(() => getUiData(props.message));
const columns = computed(() => (Array.isArray(data.value.columns) ? data.value.columns : []));
const rows = computed(() => (Array.isArray(data.value.rows) ? data.value.rows : []));

function columnKey(column: unknown, index: number) {
  return isRecord(column) ? String(column.key ?? index) : String(index);
}

function columnTitle(column: unknown, index: number) {
  const raw = isRecord(column)
    ? String(column.title ?? column.label ?? column.key ?? `列 ${index + 1}`)
    : String(column);
  // 历史消息可能只保存了英文字段名，统一映射为中文表头
  return fieldLabel(raw);
}

function cellValue(row: unknown, column: unknown, index: number) {
  if (!isRecord(row)) return '—';
  const key = isRecord(column) ? String(column.key ?? '') : String(column);
  return displayValue(row[key] ?? row[index]);
}
</script>

<template>
  <section class="ui-card ui-table-card">
    <div v-if="data.title" class="ui-card__head">
      <div class="ui-card__title">{{ data.title }}</div>
      <span class="ui-card__mark"><IconifyIcon icon="lucide:table-2"/></span>
    </div>
    <div class="ui-table-scroll">
      <table v-if="columns.length" class="ui-table">
        <thead>
          <tr>
            <th v-for="(column, index) in columns" :key="columnKey(column, index)">
              {{ columnTitle(column, index) }}
            </th>
          </tr>
        </thead>
        <tbody v-if="rows.length">
          <tr v-for="(row, rowIndex) in rows" :key="rowIndex">
            <td v-for="(column, index) in columns" :key="columnKey(column, index)">
              {{ cellValue(row, column, index) }}
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr><td :colspan="columns.length" class="ui-table__empty">
            {{ data.emptyText || '暂无数据' }}
          </td></tr>
        </tbody>
      </table>
      <div v-else class="ui-card__empty">暂无可展示的数据</div>
    </div>
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
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px 13px;
  border-bottom: 1px solid hsl(var(--border) / 0.75);
}

.ui-card__title {
  font-size: 15px;
  font-weight: 650;
}

.ui-card__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: hsl(var(--primary));
  border-radius: 9px;
  background: hsl(var(--primary) / 0.1);
}

.ui-card__mark :deep(svg) {
  width: 16px;
  height: 16px;
}

.ui-table-scroll {
  overflow-x: auto;
  scrollbar-width: thin;
}

.ui-table {
  width: 100%;
  min-width: 580px;
  border-collapse: collapse;
  font-size: 13px;
  text-align: left;
}

.ui-table th,
.ui-table td {
  padding: 12px 16px;
  border-bottom: 1px solid hsl(var(--border) / 0.7);
  vertical-align: top;
  white-space: nowrap;
}

.ui-table th {
  font-size: 11px;
  font-weight: 650;
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.07);
}

.ui-table td {
  color: hsl(var(--foreground));
}

.ui-table tbody tr:last-child td {
  border-bottom: 0;
}

.ui-table tbody tr:hover td {
  background: hsl(var(--muted) / 0.55);
}

.ui-table__empty {
  padding: 24px !important;
  color: hsl(var(--muted-foreground)) !important;
  text-align: center;
}

.ui-card__empty {
  padding: 20px 18px;
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}
</style>
