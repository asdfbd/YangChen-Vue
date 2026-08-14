<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue';

import {IconifyIcon} from '@vben/icons';
import {Empty, Input, Tooltip, Tree} from 'ant-design-vue';

defineOptions({name: 'TreePanel'});

type Key = string | number;

/** RuoYi TreeSelect 节点结构 */
interface RawNode {
  children?: RawNode[];
  disabled?: boolean;
  id: Key;
  label: string;
}

/** antd Tree DataNode 结构 */
interface AntdNode {
  children?: AntdNode[];
  disabled?: boolean;
  key: Key;
  title: string;
}

const props = withDefaults(
  defineProps<{
    /** 面板标题 */
    title?: string;
    /** 树数据（RuoYi TreeSelect 结构 id/label/children） */
    treeData?: RawNode[];
    /** 搜索框占位 */
    searchPlaceholder?: string;
    /** 提供时宽度持久化到 localStorage（tree-panel:{storageKey}） */
    storageKey?: string;
    /** 受控选中节点 key（高亮显示） */
    selectedKey?: Key;
    /** 初始宽度（px），默认 240 */
    width?: number;
    minWidth?: number;
    maxWidth?: number;
  }>(),
  {
    maxWidth: 480,
    minWidth: 180,
    searchPlaceholder: '请输入关键字',
    treeData: () => [],
    title: '',
    width: 240,
  },
);

const emit = defineEmits<{
  /** 点击树节点 */
  (e: 'node-click', node: RawNode): void;
  /** 点击刷新按钮 */
  (e: 'refresh'): void;
}>();

const keyword = ref('');
const panelWidth = ref(props.width);
const dragging = ref(false);
const expandedKeys = ref<Key[]>([]);
/** 记录 antd key -> 原始节点的映射，用于把点击回传给父级 */
const rawMap = new Map<Key, RawNode>();

const emptyImage = (Empty as any).PRESENTED_IMAGE_SIMPLE;

function toAntd(nodes: RawNode[]): AntdNode[] {
  return (nodes ?? []).map((node) => {
    rawMap.set(node.id, node);
    return {
      children: node.children?.length ? toAntd(node.children) : undefined,
      disabled: node.disabled,
      key: node.id,
      title: node.label,
    };
  });
}

function collectKeys(nodes: AntdNode[]): Key[] {
  const keys: Key[] = [];
  for (const node of nodes) {
    keys.push(node.key);
    if (node.children) keys.push(...collectKeys(node.children));
  }
  return keys;
}

function filterNodes(nodes: AntdNode[], kw: string): AntdNode[] {
  const k = kw.trim().toLowerCase();
  if (!k) return nodes;
  const out: AntdNode[] = [];
  for (const node of nodes) {
    const children = node.children ? filterNodes(node.children, k) : [];
    if ((node.title ?? '').toLowerCase().includes(k) || children.length) {
      out.push({...node, children});
    }
  }
  return out;
}

const treeData = computed(() => {
  rawMap.clear();
  return filterNodes(toAntd(props.treeData ?? []), keyword.value);
});

// 搜索时自动展开命中节点，避免过滤后默认收起
watch(
  treeData,
  (nodes) => {
    expandedKeys.value = collectKeys(nodes);
  },
  {immediate: true},
);

function handleSelect(keys: Key[]) {
  if (!keys.length) return;
  const key = keys[0] as Key;
  const node = rawMap.get(key);
  if (node) emit('node-click', node);
}

function startResize(event: MouseEvent) {
  dragging.value = true;
  const startX = event.clientX;
  const startWidth = panelWidth.value;
  const onMove = (ev: MouseEvent) => {
    panelWidth.value = Math.min(
      props.maxWidth,
      Math.max(props.minWidth, startWidth + (ev.clientX - startX)),
    );
  };
  const onUp = () => {
    dragging.value = false;
    document.removeEventListener('mousemove', onMove);
    document.removeEventListener('mouseup', onUp);
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
    if (props.storageKey) {
      try {
        localStorage.setItem(
          `tree-panel:${props.storageKey}`,
          String(panelWidth.value),
        );
      } catch {
        /* ignore */
      }
    }
  };
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  document.addEventListener('mousemove', onMove);
  document.addEventListener('mouseup', onUp);
}

onMounted(() => {
  if (props.storageKey) {
    try {
      const saved = Number(
        localStorage.getItem(`tree-panel:${props.storageKey}`),
      );
      if (saved && saved >= props.minWidth && saved <= props.maxWidth) {
        panelWidth.value = saved;
      }
    } catch {
      /* ignore */
    }
  }
});
</script>

<template>
  <aside
    :class="{ 'is-resizing': dragging }"
    :style="{ width: `${panelWidth}px` }"
    class="tree-panel"
  >
    <div class="tree-panel__header">
      <span class="tree-panel__title">
        <slot name="title-icon">
          <IconifyIcon class="tree-panel__title-icon" icon="lucide:building-2"/>
        </slot>
        {{ title }}
      </span>
      <Tooltip title="刷新">
        <button
          class="tree-panel__refresh"
          type="button"
          @click="emit('refresh')"
        >
          <IconifyIcon class="tree-panel__icon" icon="lucide:refresh-cw"/>
        </button>
      </Tooltip>
    </div>
    <div class="tree-panel__search">
      <Input v-model:value="keyword" :placeholder="searchPlaceholder" allow-clear/>
    </div>
    <div class="tree-panel__body">
      <Tree
        v-if="treeData.length"
        :expanded-keys="expandedKeys"
        :selected-keys="selectedKey ? [selectedKey] : []"
        :tree-data="treeData"
        block-node
        @expand="(keys) => (expandedKeys = keys)"
        @select="handleSelect"
      />
      <Empty v-else :image="emptyImage" description="暂无部门"/>
    </div>
    <div
      class="tree-panel__resizer"
      title="拖拽调整宽度"
      @mousedown.prevent="startResize"
    />
  </aside>
</template>

<style scoped>
.tree-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.tree-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.tree-panel__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

.tree-panel__title-icon {
  width: 16px;
  height: 16px;
  color: #1677ff;
}

.tree-panel__refresh {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  color: #4b5563;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.tree-panel__refresh:hover {
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
}

.tree-panel__icon {
  width: 14px;
  height: 14px;
}

.tree-panel__search {
  padding: 12px;
}

.tree-panel__body {
  flex: 1;
  min-height: 0;
  padding: 4px 8px 12px;
  overflow: auto;
}

.tree-panel__resizer {
  position: absolute;
  top: 0;
  right: -3px;
  z-index: 3;
  width: 6px;
  height: 100%;
  cursor: col-resize;
}

.tree-panel.is-resizing .tree-panel__resizer::after {
  content: '';
  position: absolute;
  top: 0;
  right: 2px;
  width: 2px;
  height: 100%;
  background: #1677ff;
}
</style>
