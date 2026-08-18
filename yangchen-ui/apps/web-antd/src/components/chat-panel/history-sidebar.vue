<script lang="ts" setup>
import {IconifyIcon} from '@vben/icons';
import {Modal} from 'ant-design-vue';

import type {ChatHistoryConversation} from './chat-history';

defineOptions({name: 'HistorySidebar'});

/**
 * 历史会话侧栏（DeepSeek / ChatGPT 式）
 *
 * - 展开态：历史对话列表（标题 + 相对时间 + 悬停删除）
 * - 折叠态：窄条（展开按钮 + 新对话按钮）
 * - `collapsible={false}` 时始终展开（悬浮助手弹层等场景复用）
 */
const props = withDefaults(
  defineProps<{
    conversations: ChatHistoryConversation[];
    activeId?: string | null;
    /** 折叠状态（v-model:collapsed） */
    collapsed?: boolean;
    /** 是否允许折叠 */
    collapsible?: boolean;
    /** 侧栏标题 */
    title?: string;
  }>(),
  {
    activeId: null,
    collapsed: false,
    collapsible: true,
    title: '历史对话',
  },
);

const emit = defineEmits<{
  'update:collapsed': [value: boolean];
  select: [id: string];
  remove: [id: string];
  'new-chat': [];
}>();

function onRemove(id: string) {
  const conversation = props.conversations.find((item) => item.id === id);
  Modal.confirm({
    cancelText: '取消',
    content: `“${conversation?.title || '当前对话'}”及其全部聊天记录将被删除。`,
    okText: '删除',
    okType: 'danger',
    onOk: () => emit('remove', id),
    title: '确认删除对话？',
  });
}

function toggle() {
  emit('update:collapsed', !props.collapsed);
}

/** 相对时间：今天 / 昨天显示时刻，今年内显示月-日，更早显示完整日期 */
function formatTime(ts: number): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  const now = new Date();
  const d = new Date(ts);
  const sameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate();
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  if (sameDay(now, d)) return `今天 ${hm}`;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (sameDay(yesterday, d)) return `昨天 ${hm}`;
  if (now.getFullYear() === d.getFullYear()) {
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
</script>

<template>
  <aside
    :class="['hs', {'hs--collapsed': collapsible && collapsed}]"
    aria-label="历史对话"
  >
    <!-- ===== 折叠窄条 ===== -->
    <template v-if="collapsible && collapsed">
      <div class="hs-rail">
        <button
          class="hs-rail__btn"
          title="展开历史"
          aria-label="展开历史"
          @click="toggle"
        >
          <IconifyIcon icon="lucide:panel-left-open"/>
        </button>
        <button
          class="hs-rail__btn hs-rail__btn--new"
          title="新对话"
          aria-label="新对话"
          @click="emit('new-chat')"
        >
          <IconifyIcon icon="lucide:message-square-plus"/>
        </button>
      </div>
    </template>

    <!-- ===== 展开列表 ===== -->
    <template v-else>
      <div class="hs-head">
        <span class="hs-head__title">{{ title }}</span>
        <span v-if="conversations.length" class="hs-head__count">
          {{ conversations.length }}
        </span>
        <button
          v-if="collapsible"
          class="hs-head__toggle"
          title="收起历史"
          aria-label="收起历史"
          @click="toggle"
        >
          <IconifyIcon icon="lucide:panel-left-close"/>
        </button>
      </div>

      <button class="hs-new" @click="emit('new-chat')">
        <IconifyIcon icon="lucide:message-square-plus" class="hs-new__icon"/>
        <span>新对话</span>
      </button>

      <div class="hs-list">
        <div
          v-for="c in conversations"
          :key="c.id"
          :class="['hs-item', {'hs-item--active': c.id === activeId}]"
        >
          <button class="hs-item__main" @click="emit('select', c.id)">
            <span class="hs-item__title">{{ c.title }}</span>
            <span class="hs-item__time">{{ formatTime(c.updatedAt) }}</span>
          </button>
          <button
            class="hs-item__del"
            title="删除会话"
            aria-label="删除会话"
            @click="onRemove(c.id)"
          >
            <IconifyIcon icon="lucide:trash-2"/>
          </button>
        </div>

        <div v-if="!conversations.length" class="hs-empty">
          <IconifyIcon icon="lucide:history" class="hs-empty__icon"/>
          <p class="hs-empty__title">暂无历史对话</p>
          <p class="hs-empty__sub">开始第一段对话后自动保存</p>
        </div>
      </div>
    </template>
  </aside>
</template>

<style scoped>
.hs {
  --hs-text: hsl(var(--foreground));
  --hs-sub: hsl(var(--muted-foreground));
  --hs-line: hsl(var(--border));
  --hs-primary: hsl(var(--primary));

  display: flex;
  flex-direction: column;
  width: 264px;
  height: 100%;
  min-height: 0;
  background: hsl(var(--card) / 0.92);
  border-right: 1px solid var(--hs-line);
}

/* ===== 折叠窄条 ===== */
.hs--collapsed {
  width: 58px;
}

.hs-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 0;
}

.hs-rail__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  color: var(--hs-sub);
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s;
}

.hs-rail__btn:hover {
  color: var(--hs-text);
  background: hsl(var(--muted));
}

.hs-rail__btn--new {
  color: hsl(var(--primary-foreground));
  background: var(--hs-primary);
  box-shadow: 0 8px 18px -6px hsl(var(--primary) / 0.5);
}

.hs-rail__btn--new:hover {
  color: hsl(var(--primary-foreground));
  background: var(--hs-primary);
  opacity: 0.9;
}

.hs-rail__btn :deep(svg) {
  width: 17px;
  height: 17px;
}

/* ===== 头部 ===== */
.hs-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  padding: 18px 16px 8px;
}

.hs-head__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--hs-sub);
  letter-spacing: 1px;
}

.hs-head__count {
  min-width: 18px;
  padding: 1px 6px;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  color: var(--hs-sub);
  border-radius: 99px;
  background: hsl(var(--muted));
}

.hs-head__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  margin-left: auto;
  padding: 0;
  color: var(--hs-sub);
  border: none;
  border-radius: 7px;
  background: transparent;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s;
}

.hs-head__toggle:hover {
  color: var(--hs-text);
  background: hsl(var(--muted));
}

.hs-head__toggle :deep(svg) {
  width: 14px;
  height: 14px;
}

/* ===== 新对话按钮 ===== */
.hs-new {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 9px;
  margin: 8px 14px 12px;
  padding: 10px 14px;
  font-size: 14px;
  font-weight: 500;
  color: var(--hs-text);
  border: 1px solid var(--hs-line);
  border-radius: 10px;
  background: hsl(var(--background-deep) / 0.36);
  cursor: pointer;
  transition:
    border-color 0.16s,
    background 0.16s,
    color 0.16s;
}

.hs-new:hover {
  color: hsl(var(--primary-foreground));
  border-color: var(--hs-primary);
  background: var(--hs-primary);
}

.hs-new__icon :deep(svg) {
  width: 15px;
  height: 15px;
}

/* ===== 会话列表 ===== */
.hs-list {
  flex: 1;
  min-height: 0;
  padding: 0 8px 8px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(107, 114, 128, 0.28) transparent;
}

.hs-list::-webkit-scrollbar {
  width: 5px;
}

.hs-list::-webkit-scrollbar-thumb {
  border-radius: 99px;
  background: rgba(107, 114, 128, 0.28);
}

.hs-item {
  display: flex;
  align-items: center;
  margin-bottom: 2px;
  border-radius: 9px;
  transition: background 0.15s;
}

.hs-item:hover {
  background: hsl(var(--muted));
}

.hs-item--active {
  background: hsl(var(--primary) / 0.08);
  box-shadow: inset 3px 0 0 hsl(var(--primary));
}

.hs-item--active:hover {
  background: hsl(var(--primary) / 0.12);
}

.hs-item__main {
  flex: 1;
  min-width: 0;
  padding: 10px 4px 10px 12px;
  text-align: left;
  border: none;
  background: transparent;
  cursor: pointer;
}

.hs-item__title {
  display: block;
  overflow: hidden;
  font-size: 13.5px;
  line-height: 1.4;
  color: var(--hs-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hs-item--active .hs-item__title {
  color: var(--hs-primary);
  font-weight: 500;
}

.hs-item__time {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  color: var(--hs-sub);
}

.hs-item__del {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-right: 6px;
  padding: 0;
  color: var(--hs-sub);
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  opacity: 0;
  transition:
    opacity 0.15s,
    color 0.15s,
    background 0.15s;
}

.hs-item:hover .hs-item__del {
  opacity: 1;
}

.hs-item__del:hover {
  color: hsl(0 72% 51%);
  background: hsl(0 72% 51% / 0.1);
}

.hs-item__del :deep(svg) {
  width: 14px;
  height: 14px;
}

/* 空状态 */
.hs-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 46px 16px;
  text-align: center;
}

.hs-empty__icon {
  color: var(--hs-sub);
  opacity: 0.5;
}

.hs-empty__icon :deep(svg) {
  width: 30px;
  height: 30px;
}

.hs-empty__title {
  margin-top: 12px;
  font-size: 13px;
  color: var(--hs-text);
}

.hs-empty__sub {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--hs-sub);
}

</style>
