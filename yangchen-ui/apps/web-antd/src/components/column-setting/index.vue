<script lang="ts" setup>
import {computed, onMounted} from 'vue';

import {IconifyIcon} from '@vben/icons';
import type {TableColumnType} from 'ant-design-vue';
import {Button, Checkbox, Popover} from 'ant-design-vue';

defineOptions({name: 'ColumnSetting'});

type Col = TableColumnType & { visible?: boolean };

const props = withDefaults(
  defineProps<{
    /** 列配置（响应式数组，本组件直接修改其中列的 visible） */
    columns: Col[];
    /** 过滤哪些列可被切换（如排除操作列） */
    filter?: (col: Col) => boolean;
    /** 提供时列显隐持久化到 localStorage（col-setting:{storageKey}） */
    storageKey?: string;
  }>(),
  {filter: () => true},
);

const STORAGE_PREFIX = 'col-setting:';

const toggleable = computed(() => props.columns.filter(props.filter));

onMounted(() => {
  if (!props.storageKey) return;
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + props.storageKey);
    if (!raw) return;
    const map = JSON.parse(raw) as Record<string, boolean>;
    for (const col of props.columns) {
      if (col.key != null && map[String(col.key)] != null) {
        col.visible = map[String(col.key)];
      }
    }
  } catch {
    /* ignore */
  }
});

function toggle(col: Col, event: any) {
  col.visible = Boolean(event?.target?.checked);
  if (!props.storageKey) return;
  try {
    const map: Record<string, boolean> = {};
    for (const c of props.columns) {
      if (c.key != null) map[String(c.key)] = c.visible !== false;
    }
    localStorage.setItem(STORAGE_PREFIX + props.storageKey, JSON.stringify(map));
  } catch {
    /* ignore */
  }
}
</script>

<template>
  <Popover placement="bottomRight" trigger="click">
    <template #content>
      <div class="col-setting">
        <div class="col-setting__title">列设置</div>
        <Checkbox
          v-for="col in toggleable"
          :key="col.key"
          :checked="col.visible !== false"
          @change="toggle(col, $event)"
        >
          {{ col.title }}
        </Checkbox>
      </div>
    </template>
    <Button size="small">
      <IconifyIcon class="btn-icon" icon="lucide:settings-2"/>
      列设置
    </Button>
  </Popover>
</template>

<style scoped>
.col-setting {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 140px;
  padding: 4px;
}

.col-setting__title {
  margin-bottom: 2px;
  padding-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.45);
  border-bottom: 1px solid #f0f0f0;
}

.btn-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 6px;
  vertical-align: -2px;
}
</style>
