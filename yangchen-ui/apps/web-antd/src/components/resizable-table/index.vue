<script lang="ts" setup>
import type {TableColumnType} from 'ant-design-vue';

import {computed, onMounted, onUnmounted, ref, useAttrs, useSlots} from 'vue';

import {Table} from 'ant-design-vue';

defineOptions({name: 'ResizableTable', inheritAttrs: false});

const props = withDefaults(
  defineProps<{
    /**
     * 列配置。必须是页面持有的响应式数组（ref），本组件会直接修改其中列的 width。
     * 列可额外配置 resizable: false 表示该列不可拖拽调宽。
     */
    columns: Col[];
    /** 无 width 的列拖拽时的起始宽度（px），默认 120 */
    defaultWidth?: number;
    /** 拖拽时的最小列宽（px），默认 80 */
    minWidth?: number;
    /** 是否允许列拖拽调宽，默认 true */
    resizable?: boolean;
    /** 提供时列宽持久化到 localStorage（rt-table:{storageKey}） */
    storageKey?: string;
  }>(),
  {defaultWidth: 120, minWidth: 80, resizable: true, storageKey: ''},
);

type Col = TableColumnType & { resizable?: boolean; visible?: boolean };

const STORAGE_PREFIX = 'rt-table:';

const slots = useSlots();
const hasBodySlot = computed(() => Boolean(slots.bodyCell));
const hasEmptySlot = computed(() => Boolean(slots.emptyText));
const hasSummarySlot = computed(() => Boolean(slots.summary));

const resizing = ref(false);

// —— 表体自适应滚动：占满剩余高度、表头固定、内部滚动 ——
// 用法：外层是一个 flex 列容器（如卡片体），本组件根节点 flex:1 撑满，
// 用 ResizeObserver 测量可用高度并给 antd Table 设 scroll.y，实现「表格内滚动」。
const wrapperRef = ref<HTMLElement | null>(null);
const scrollY = ref<number | undefined>(undefined);
let resizeObserver: null | ResizeObserver = null;
let mutationObserver: MutationObserver | null = null;
let resizeTimer: number | undefined;

const attrs = useAttrs();
// 父级可能传 scroll.x（横向滚动）。scroll 必须由本组件与 scrollY 合并后显式传递，
// 否则 v-bind="$attrs" 会覆盖本组件的 scroll.y，导致表体固定不滚动。
const tableAttrs = computed(() => {
  const {scroll: _omit, ...rest} = attrs;
  return rest;
});
const tableScroll = computed(() => {
  const scroll = attrs.scroll as undefined | { x?: number | string };
  return {...scroll, y: scrollY.value};
});

/**
 * 计算表体可用高度：表体高度 = 容器高度 - 表头 - 分页。
 * 数据少时取内容自然高度（表格紧凑不撑开），数据超过可用高度时取可用高度（表内滚动）。
 */
function calcScrollY() {
  const el = wrapperRef.value;
  if (!el) return;
  const wrapHeight = el.clientHeight;
  if (!wrapHeight) return;
  const headerEl = el.querySelector<HTMLElement>('.ant-table-thead');
  const headerHeight = headerEl ? headerEl.offsetHeight : 0;
  const paginationEl = el.querySelector<HTMLElement>('.ant-pagination');
  // 分页距表格约 16px 上边距
  const paginationHeight = paginationEl ? paginationEl.offsetHeight + 16 : 0;
  const available = Math.floor(wrapHeight - headerHeight - paginationHeight);
  if (available <= 0) {
    scrollY.value = undefined;
    return;
  }
  // 已渲染出滚动体（.ant-table-body）时，scrollHeight 即内容自然高度
  const bodyEl = el.querySelector<HTMLElement>('.ant-table-body');
  const natural =
    bodyEl && bodyEl.scrollHeight > 0 ? bodyEl.scrollHeight : available;
  scrollY.value = Math.min(available, natural);
}

function scheduleCalcScrollY() {
  if (resizeTimer) window.clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => {
    resizeTimer = undefined;
    calcScrollY();
  }, 50);
}

function isResizable(col: any) {
  return props.resizable !== false && col?.resizable !== false;
}

function loadWidths() {
  if (!props.storageKey) return;
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + props.storageKey);
    if (!raw) return;
    const map = JSON.parse(raw) as Record<string, number>;
    for (const col of props.columns) {
      const width = map[String(col.key)];
      if (col.key != null && width && isResizable(col)) {
        col.width = width;
      }
    }
  } catch {
    /* localStorage 不可用则忽略 */
  }
}

function saveWidths() {
  if (!props.storageKey) return;
  const map: Record<string, number> = {};
  for (const col of props.columns) {
    if (col.key != null && isResizable(col) && col.width != null) {
      map[String(col.key)] = Math.round(Number(col.width));
    }
  }
  try {
    localStorage.setItem(STORAGE_PREFIX + props.storageKey, JSON.stringify(map));
  } catch {
    /* ignore */
  }
}

let drag: null | { column: Col; startWidth: number; startX: number } = null;

function startResize(column: any, event: MouseEvent) {
  if (!isResizable(column)) return;
  const startWidth = Number(column.width) || props.defaultWidth;
  drag = {column, startWidth, startX: event.clientX};
  resizing.value = true;
  document.addEventListener('mousemove', onMove);
  document.addEventListener('mouseup', onUp);
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
}

function onMove(event: MouseEvent) {
  if (!drag) return;
  const width = Math.max(
    props.minWidth,
    Math.round(drag.startWidth + event.clientX - drag.startX),
  );
  drag.column.width = width;
}

function onUp() {
  if (!drag) return;
  drag = null;
  resizing.value = false;
  document.removeEventListener('mousemove', onMove);
  document.removeEventListener('mouseup', onUp);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
  saveWidths();
}

onMounted(() => {
  loadWidths();
  if (wrapperRef.value) {
    resizeObserver = new ResizeObserver(scheduleCalcScrollY);
    resizeObserver.observe(wrapperRef.value);
    // 监听表格行增删/数据变化，重算滚动高度
    mutationObserver = new MutationObserver(scheduleCalcScrollY);
    mutationObserver.observe(wrapperRef.value, {
      childList: true,
      subtree: true,
    });
  }
  calcScrollY();
});

onUnmounted(() => {
  resizeObserver?.disconnect();
  mutationObserver?.disconnect();
  resizeObserver = null;
  mutationObserver = null;
  if (resizeTimer) window.clearTimeout(resizeTimer);
});
</script>

<template>
  <div ref="wrapperRef" :class="{ 'is-resizing': resizing }" class="rt-table">
    <Table
      :columns="columns"
      :scroll="tableScroll"
      v-bind="tableAttrs"
    >
      <!-- 表头单元格：右侧渲染拖拽手柄，鼠标按住左右拖动调整列宽 -->
      <template #headerCell="{ title, column }">
        <span class="rt-th">
          {{ title }}
          <i
            v-if="isResizable(column)"
            class="rt-resize-handle"
            @mousedown.prevent.stop="startResize(column, $event)"
          ></i>
        </span>
      </template>
      <template v-if="hasBodySlot" #bodyCell="scope">
        <slot name="bodyCell" v-bind="scope"></slot>
      </template>
      <template v-if="hasEmptySlot" #emptyText="scope">
        <slot name="emptyText" v-bind="scope"></slot>
      </template>
      <template v-if="hasSummarySlot" #summary="scope">
        <slot name="summary" v-bind="scope"></slot>
      </template>
    </Table>
  </div>
</template>

<style scoped>
/* 在 flex 列容器中占满剩余高度，内部滚动由 antd Table scroll.y 处理 */
.rt-table {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.rt-table :deep(.ant-table-wrapper) {
  flex: 1;
  min-height: 0;
}

.rt-table :deep(.ant-table-thead > tr > th) {
  position: relative;
}

.rt-th {
  display: inline-flex;
  align-items: center;
  width: 100%;
}

.rt-resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  width: 10px;
  height: 100%;
  cursor: col-resize;
  touch-action: none;
}

.rt-resize-handle::after {
  content: '';
  position: absolute;
  top: 22%;
  right: 3px;
  width: 2px;
  height: 56%;
  border-radius: 1px;
  background: transparent;
  transition: background-color 0.15s ease;
}

.rt-table.is-resizing .rt-resize-handle::after,
.rt-resize-handle:hover::after {
  background: #1677ff;
}

.rt-table.is-resizing {
  cursor: col-resize;
}
</style>
