<script lang="ts" setup>
import {IconifyIcon} from '@vben/icons';

import ChatMessageText from './message-text.vue';
import ConfirmCard from './renderers/confirm-card.vue';
import ChoiceCard from './renderers/choice-card.vue';
import DataTable from './renderers/data-table.vue';
import DetailCard from './renderers/detail-card.vue';
import DynamicForm from './renderers/dynamic-form.vue';
import ErrorCard from './renderers/error-card.vue';
import ResultCard from './renderers/result-card.vue';
import StatCard from './renderers/stat-card.vue';

import type {ChatMessage, ChatRendererMap, ChatUiAction} from './types';

defineOptions({name: 'ChatMessageList'});

/**
 * 消息列表（纯渲染）
 * - variant="panel"：紧凑气泡（悬浮小窗形态）
 * - variant="page"：全页流式排版（助手=无框文本，用户=右侧柔和药丸）
 * 滚动由宿主容器负责
 */
const props = withDefaults(
  defineProps<{
    messages: ChatMessage[];
    busy?: boolean;
    avatarIcon?: string;
    renderers?: ChatRendererMap;
    variant?: 'panel' | 'page';
    /** 正在流式输出的助手消息 id（用于显示打字光标） */
    streamingId?: string | null;
  }>(),
  {
    busy: false,
    avatarIcon: 'lucide:sparkles',
    renderers: () => ({}),
    variant: 'panel',
    streamingId: null,
  },
);

const emit = defineEmits<{
  'ui-action': [payload: ChatUiAction];
}>();

const builtInRenderers: ChatRendererMap = {
  detail: DetailCard,
  table: DataTable,
  stat: StatCard,
  confirm: ConfirmCard,
  select: ChoiceCard,
  form: DynamicForm,
  result: ResultCard,
  error: ErrorCard,
};

/** 自定义渲染器是否命中该消息类型 */
function hasRenderer(msg: ChatMessage): boolean {
  return Boolean(msg.type && (props.renderers[msg.type] || builtInRenderers[msg.type]));
}

function rendererFor(msg: ChatMessage) {
  return (msg.type && (props.renderers[msg.type] || builtInRenderers[msg.type])) || ChatMessageText;
}

/** 默认文本渲染器：流式消息附带光标指示（自定义渲染器不传，避免多余属性） */
function textRendererProps(msg: ChatMessage) {
  return rendererFor(msg) === ChatMessageText
    ? {message: msg, streaming: msg.id === props.streamingId}
    : rendererFor(msg) === ChoiceCard
      ? {message: msg, disabled: props.busy}
    : {message: msg};
}

function handleUiAction(
  message: ChatMessage,
  payload: Omit<ChatUiAction, 'message'>,
) {
  emit('ui-action', {...payload, message});
}
</script>

<template>
  <div :class="['cml-list', `cml-list--${variant}`]">
    <TransitionGroup name="cml-msg">
      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['cml-msg', `cml-msg--${msg.role}`, {'is-card': hasRenderer(msg)}]"
      >
        <template v-if="msg.role === 'assistant'">
          <span class="cml-avatar">
            <IconifyIcon :icon="avatarIcon"/>
          </span>
          <div :class="['cml-body', {'cml-body--wide': hasRenderer(msg)}]">
            <component
              :is="rendererFor(msg)"
              v-bind="textRendererProps(msg)"
              @ui-action="handleUiAction(msg, $event)"
            />
            <span class="cml-time">{{ msg.time }}</span>
          </div>
        </template>
        <template v-else>
          <div class="cml-body cml-body--user">
            <div class="cml-text">{{ msg.content }}</div>
            <span class="cml-time">{{ msg.time }}</span>
          </div>
          <span v-if="variant === 'panel'" class="cml-avatar cml-avatar--user">
            <IconifyIcon icon="lucide:user"/>
          </span>
        </template>
      </div>
    </TransitionGroup>

    <!-- 正在输入 -->
    <Transition name="cml-msg">
      <div v-if="busy" class="cml-msg cml-msg--assistant">
        <span class="cml-avatar">
          <IconifyIcon :icon="avatarIcon"/>
        </span>
        <div class="cml-body cml-body--typing">
          <span class="cml-dot"></span>
          <span class="cml-dot"></span>
          <span class="cml-dot"></span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.cml-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.cml-msg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.cml-msg--user {
  flex-direction: row-reverse;
}

.cml-avatar {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
}

.cml-msg--assistant > .cml-avatar {
  color: hsl(var(--primary-foreground));
  background: hsl(var(--primary));
  box-shadow: 0 4px 10px -5px hsl(var(--primary) / 0.55);
}

.cml-msg--user > .cml-avatar {
  color: hsl(var(--muted-foreground));
  border: 1px solid hsl(var(--border));
  background: hsl(var(--muted));
}

.cml-avatar :deep(svg) {
  width: 14px;
  height: 14px;
}

.cml-time {
  display: block;
  margin-top: 5px;
  font-size: 10px;
  opacity: 0.5;
}

/* ===== 形态一：panel（紧凑气泡） ===== */
.cml-list--panel .cml-body {
  max-width: 80%;
  padding: 11px 15px;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}

.cml-list--panel .cml-msg--assistant .cml-body {
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
  border-radius: 6px 18px 18px 18px;
  background: hsl(var(--muted));
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.cml-list--panel .cml-msg--user .cml-body {
  color: hsl(var(--primary-foreground));
  border-radius: 18px 6px 18px 18px;
  background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.18),
      transparent 42%,
      rgba(0, 0, 0, 0.16)
    ),
    hsl(var(--primary));
  box-shadow: 0 6px 16px -8px hsl(var(--primary) / 0.45);
}

.cml-list--panel .cml-body--wide {
  max-width: 97%;
}

.cml-list--panel .cml-msg--user .cml-text :deep(strong) {
  color: hsl(var(--primary-foreground));
}

/* ===== 形态二：page（全页流式排版） ===== */
.cml-list--page .cml-msg {
  gap: 14px;
}

.cml-list--page .cml-avatar {
  width: 32px;
  height: 32px;
  margin-top: 2px;
}

.cml-list--page .cml-body {
  min-width: 0;
  font-size: 16px;
  line-height: 1.85;
  word-break: break-word;
}

/* 助手消息：无框流式文本 */
.cml-list--page .cml-msg--assistant .cml-body {
  flex: 1;
  padding: 2px 0;
  color: hsl(var(--foreground));
}

/* 用户消息：右侧柔和药丸 */
.cml-list--page .cml-msg--user {
  justify-content: flex-end;
}

.cml-list--page .cml-msg--user .cml-body {
  max-width: 82%;
  padding: 10px 18px;
  font-size: 15px;
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--primary) / 0.12);
  border-radius: 16px 5px 16px 16px;
  background: hsl(var(--primary) / 0.08);
}

.cml-list--page .cml-msg--user .cml-text :deep(strong) {
  color: hsl(var(--primary));
}

.cml-list--page .cml-body--wide {
  flex: 1;
}

/* 正在输入 */
.cml-body--typing {
  display: flex;
  gap: 5px;
  padding: 13px 15px;
}

.cml-list--page .cml-body--typing {
  padding: 8px 0;
}

.cml-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: hsl(var(--primary));
  animation: cml-blink 1.1s infinite ease-in-out;
}

.cml-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.cml-dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes cml-blink {
  0%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

/* ===== 过渡 ===== */
.cml-msg-enter-active {
  transition: all 0.28s cubic-bezier(0.2, 0.8, 0.3, 1);
}

.cml-msg-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

@media (prefers-reduced-motion: reduce) {
  .cml-msg {
    transition: none !important;
  }

  .cml-dot {
    animation: none !important;
  }
}
</style>
